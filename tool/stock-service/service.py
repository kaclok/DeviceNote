"""
股票行情数据层：封装 levistock，产出前端直接可用的结构化数据。

约定：
- 所有函数接收 `date`（"YYYY-MM-DD" 或 "YYYYMMDD"，不传=今天），内部按数据源要求转换格式；
- 返回值为纯 dict / list，金额单位一律「元」，前端负责换算亿/万；
- 只做取数与聚合，不抛业务包装异常：取数失败由上层（main.py）转 HTTP 错误；
- 两个可调项走环境变量（缺省值 = 原行为，由启动器注入，也可自行设置）：
    STOCK_LIB_DIR            Ashare 单文件库所在目录（缺省 = 本文件同级的 lib/）
    STOCK_CACHE_TTL_SCALE    缓存 TTL 倍率（缺省 1；0 = 关闭缓存；2 = 全部加倍）
"""

import os

# 必须在 import levistock（其内部 import tqdm）之前设置，关闭翻页进度条，避免污染服务日志
os.environ.setdefault("TQDM_DISABLE", "1")

import re
import threading
import time
from datetime import date as date_cls
from datetime import datetime

import levistock as lk
import requests

# ---------------------------------------------------------------------------
# 缓存：行情为分钟级数据，短 TTL 缓存可显著降低对上游（东财/财联社/开盘红）的调用压力
# ---------------------------------------------------------------------------

_CACHE: dict = {}
_CACHE_LOCK = threading.Lock()


def _ttl_scale() -> float:
    """缓存 TTL 倍率：读环境变量 STOCK_CACHE_TTL_SCALE（由启动器注入）。

    缺省 1 ⇒ 各接口保持原有 TTL，单独跑 start.bat / uvicorn 时行为完全不变；
    0 ⇒ 关闭缓存（调参或排查取数问题时用）；2 ⇒ 全部 TTL 加倍。
    空值或非法值按 1 处理。
    """
    raw = os.environ.get("STOCK_CACHE_TTL_SCALE", "")
    if not raw.strip():
        return 1.0
    try:
        scale = float(raw)
    except ValueError:
        return 1.0
    return scale if scale > 0 else 0.0


def _cached(ttl: int):
    def deco(fn):
        def wrapper(*args, **kwargs):
            effective = ttl * _ttl_scale()
            if effective <= 0:          # 倍率 0 ⇒ 关闭缓存，直取上游
                return fn(*args, **kwargs)
            key = (fn.__name__, args, tuple(sorted(kwargs.items())))
            now = time.time()
            with _CACHE_LOCK:
                hit = _CACHE.get(key)
                if hit and hit[0] > now:
                    return hit[1]
            value = fn(*args, **kwargs)
            with _CACHE_LOCK:
                _CACHE[key] = (now + effective, value)
            return value

        return wrapper

    return deco


# ---------------------------------------------------------------------------
# 归因字典：把「涨停原因 / 题材 / 催化剂 / 电报」文本归类到有限的因素上
# ---------------------------------------------------------------------------

FACTORS = [
    {
        "code": "policy", "name": "政策驱动", "color": "#f59e0b",
        "keywords": ["政策", "规划", "国务院", "发改委", "工信部", "财政部", "部委", "试点",
                     "补贴", "获批", "审批", "央企", "国资", "监管", "会议", "纲要", "行动方案",
                     "指导意见", "十五五", "十四五", "国家", "专项债", "重组政策"],
    },
    {
        "code": "overseas", "name": "外围市场", "color": "#8b5cf6",
        "keywords": ["美股", "纳斯达克", "道琼斯", "隔夜", "海外", "美联储", "降息", "加息",
                     "关税", "出口", "汇率", "港股", "英伟达", "特斯拉", "台积电", "外盘"],
    },
    {
        "code": "market", "name": "市场情绪", "color": "#ef4444",
        "keywords": ["情绪", "资金", "接力", "轮动", "抱团", "主线", "人气", "游资", "连板",
                     "反包", "赚钱效应", "高位股", "补涨", "分歧"],
    },
    {
        "code": "theme", "name": "题材事件", "color": "#3b82f6",
        "keywords": ["概念", "题材", "机器人", "算力", "人工智能", "AI", "芯片", "半导体",
                     "固态电池", "锂电池", "低空", "军工", "创新药", "光伏", "储能", "液冷",
                     "光模块", "CPO", "稀土", "风能", "风电", "数据要素", "国产替代", "并购重组",
                     "短剧", "影视", "游戏", "消费电子", "有色", "煤炭", "石油", "券商"],
    },
    {
        "code": "performance", "name": "业绩驱动", "color": "#10b981",
        "keywords": ["业绩", "预增", "净利", "净利润", "扭亏", "超预期", "订单", "营收",
                     "增长", "毛利", "财报", "季报", "中报", "年报"],
    },
    {
        "code": "news", "name": "公告消息", "color": "#06b6d4",
        "keywords": ["公告", "中标", "合作", "签约", "重组", "收购", "增持", "回购", "股权",
                     "合同", "设立", "投产", "定增", "重组预案", "资产注入"],
    },
    {
        "code": "capital", "name": "资金面", "color": "#eab308",
        "keywords": ["主力", "北向", "机构", "融资", "净流入", "龙虎榜", "封单", "大单", "席位"],
    },
    {
        "code": "technical", "name": "技术形态", "color": "#64748b",
        "keywords": ["突破", "首板", "一字", "放量", "加速", "新高", "回封", "地天板", "缩量",
                     "均线", "趋势"],
    },
    {
        "code": "risk", "name": "风险出清", "color": "#94a3b8",
        "keywords": ["减持", "亏损", "退市", "立案", "问询", "下滑", "暴雷", "商誉", "违约",
                     "预亏", "解禁", "诉讼", "处罚", "风险警示", "ST"],
    },
]


# ---------------------------------------------------------------------------
# 通用小工具
# ---------------------------------------------------------------------------

def parse_date(value) -> date_cls:
    """把 "YYYY-MM-DD" / "YYYYMMDD" / 空值 统一成 date"""
    if value is None or str(value).strip() == "":
        return date_cls.today()
    text = str(value).strip()
    for pattern in ("%Y-%m-%d", "%Y%m%d", "%Y/%m/%d"):
        try:
            return datetime.strptime(text, pattern).date()
        except ValueError:
            continue
    raise ValueError(f"日期格式错误：{text}，应为 YYYY-MM-DD 或 YYYYMMDD")


def _plain_day(day: date_cls) -> str:
    return day.strftime("%Y%m%d")


def _dash_day(day: date_cls) -> str:
    return day.strftime("%Y-%m-%d")


def _num(value, default=0.0) -> float:
    """上游大量使用 "-" 表示缺失，统一兜底为数字"""
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _ratio_to_pct(value, default=0.0) -> float:
    """财联社的涨跌幅是小数（0.0677），统一转成百分数（6.77）"""
    return round(_num(value, default) * 100, 2)


def _fmt_hms(value) -> str:
    """东财/开盘红的时间是 "92500" / "093000" 这类紧凑数字，统一成 HH:MM:SS"""
    text = str(value or "").strip()
    if not text or text in ("-", "0"):
        return ""
    if text.isdigit():
        text = text.zfill(6)
        if len(text) == 6:
            return f"{text[0:2]}:{text[2:4]}:{text[4:6]}"
    return str(value)


def _fmt_epoch(value) -> str:
    """秒级时间戳 → HH:MM:SS"""
    seconds = int(_num(value, 0))
    if seconds <= 0:
        return ""
    return datetime.fromtimestamp(seconds).strftime("%H:%M:%S")


def _strip_market(code: str) -> str:
    """sz002979 / SH600000 → 002979"""
    text = str(code or "").strip()
    if len(text) > 6 and text[:2].lower() in ("sz", "sh", "bj"):
        return text[2:]
    return text


def _match_factors(text: str):
    """把一段文本归类到若干因素上"""
    hits = []
    if not text:
        return hits
    for factor in FACTORS:
        for keyword in factor["keywords"]:
            if keyword in text:
                hits.append(factor["code"])
                break
    return hits


# ---------------------------------------------------------------------------
# 交易日 / 元信息
# ---------------------------------------------------------------------------

@_cached(60)
def trade_meta() -> dict:
    today = date_cls.today()
    try:
        is_trade = bool(lk.is_trade_day())
    except Exception:
        # 交易日接口不可用时按「工作日」降级，避免整页不可用
        is_trade = today.weekday() < 5
    try:
        # 恒拉取交易日列表（非交易日子列表末尾即上一个交易日），前端默认取最大日期
        days = list(lk.get_trade_days(20))
    except Exception:
        days = []
    if is_trade and datetime.now().strftime("%H%M") >= "0915" \
            and (not days or days[-1] != _plain_day(today)):
        # 已开盘（>=09:15 集合竞价）但列表缺当天时补上
        days.append(_plain_day(today))
    if days and days[-1] == _plain_day(today) and datetime.now().strftime("%H%M") < "0915":
        # ⚠️ levistock 交易日历开盘前就把今天列进 trade_days ⇒ 剔除：
        # K 线/分时上游只有上一交易日数据，默认选今天会造成顶栏与图表不一致
        days = days[:-1]
    if not days:
        days = [_plain_day(today)]
    return {
        "today": _dash_day(today),
        "is_trade_day": is_trade,
        "trade_days": days,
    }


def _market_status(is_trade_day: bool) -> int:
    """1=交易中 2=已收盘 3=非交易日"""
    if not is_trade_day:
        return 3
    now = datetime.now().strftime("%H%M")
    return 1 if "0915" <= now <= "1505" else 2


# ---------------------------------------------------------------------------
# 原始池（带缓存）
# ---------------------------------------------------------------------------

@_cached(15)
def _zt_pool(day_text: str) -> list:
    return lk.stock_zt_pool_em(date=day_text)


@_cached(30)
def _zt_pool_cls() -> list:
    """财联社涨停池：仅当天，含涨停原因"""
    return lk.stock_zt_pool_cls()


@_cached(30)
def _limit_up_reason_map() -> dict:
    """股票名称 → 涨停原因（当天）"""
    mapping = {}
    try:
        for item in _zt_pool_cls():
            name = str(item.get("secu_name", "")).strip()
            if name:
                mapping[name] = str(item.get("up_reason", "")).strip()
    except Exception:
        return {}
    return mapping


@_cached(60)
def _wind_list() -> list:
    try:
        return list(lk.market_wind_cls())
    except Exception:
        return []


@_cached(60)
def _sector_industry() -> list:
    try:
        return list(lk.sector_industry_cls())
    except Exception:
        return []


@_cached(60)
def _sector_heat() -> list:
    try:
        return list(lk.get_sector_heat())
    except Exception:
        return []


@_cached(120)
def _news(category: str, day: date_cls) -> list:
    return list(lk.news_telegraph_cls(date=_dash_day(day), category=category))


# ---------------------------------------------------------------------------
# 归一化：涨停记录
# ---------------------------------------------------------------------------

def _norm_limit_up(item: dict, reason_map: dict) -> dict:
    name = str(item.get("stock_name", "")).strip()
    return {
        "code": str(item.get("stock_code", "")).strip(),
        "name": name,
        "price": _num(item.get("price")),
        "change_pct": round(_num(item.get("change_pct")), 2),
        "sector": str(item.get("sector", "")).strip() or "未分类",
        "boards": int(_num(item.get("continuous"), 1)) or 1,
        "first_limit_time": _fmt_hms(item.get("first_zt_time")),
        "last_limit_time": _fmt_hms(item.get("last_zt_time")),
        "broken_times": int(_num(item.get("open_times"), 0)),
        "amount": _num(item.get("amount")),
        "circ_market": _num(item.get("circ_market")),
        "turnover_rate": round(_num(item.get("turnover_rate")), 2),
        "main_inflow": _num(item.get("main_inflow")),
        "zt_days": int(_num(item.get("zt_days"), 0)),
        "zt_count": int(_num(item.get("zt_count"), 0)),
        "reason": reason_map.get(name, ""),
    }


def limit_up_list(day: date_cls) -> dict:
    raw = _zt_pool(_plain_day(day))
    reason_map = _limit_up_reason_map() if day == date_cls.today() else {}
    items = [_norm_limit_up(x, reason_map) for x in raw]
    items.sort(key=lambda s: (-s["boards"], s["first_limit_time"] or "99:99:99"))
    return {"trade_date": _dash_day(day), "total": len(items), "list": items}


def limit_up_reason(day: date_cls) -> dict:
    """财联社涨停原因（仅当天可查）"""
    if day != date_cls.today():
        return {"trade_date": _dash_day(day), "total": 0, "list": [],
                "notice": "涨停原因数据源仅提供当日数据"}
    items = []
    for item in _zt_pool_cls():
        reason = str(item.get("up_reason", "")).strip()
        theme, _, detail = reason.partition("|")
        items.append({
            "code": _strip_market(item.get("secu_code")),
            "name": str(item.get("secu_name", "")).strip(),
            "price": _num(item.get("last_px")),
            "change_pct": _ratio_to_pct(item.get("change")),
            "theme": theme.strip(),
            "detail": detail.strip(),
        })
    return {"trade_date": _dash_day(day), "total": len(items), "list": items}


def yesterday_limit_up(day: date_cls) -> dict:
    raw = lk.stock_yesterday_zt_em(date=_plain_day(day))
    items = [{
        "code": str(x.get("stock_code", "")).strip(),
        "name": str(x.get("stock_name", "")).strip(),
        "price": _num(x.get("price")),
        "change_pct": round(_num(x.get("change_pct")), 2),
        "sector": str(x.get("sector", "")).strip() or "未分类",
        "open_ratio": round(_num(x.get("open_ratio")), 2),
        "amplitude": round(_num(x.get("amplitude")), 2),
        "turnover_rate": round(_num(x.get("turnover_rate")), 2),
        "amount": _num(x.get("amount")),
        "yesterday_boards": int(_num(x.get("yesterday_cont"), 1)) or 1,
        "yesterday_time": _fmt_hms(x.get("yesterday_time")),
    } for x in raw]
    items.sort(key=lambda s: -s["change_pct"])
    return {"trade_date": _dash_day(day), "total": len(items), "list": items}


# ---------------------------------------------------------------------------
# 板块聚合
# ---------------------------------------------------------------------------

def _group_by_sector(items: list, board_key: str) -> list:
    buckets: dict = {}
    for item in items:
        sector = item.get("sector") or "未分类"
        bucket = buckets.setdefault(sector, {"name": sector, "count": 0, "stocks": []})
        bucket["count"] += 1
        bucket["stocks"].append({
            "code": item["code"],
            "name": item["name"],
            "change_pct": item["change_pct"],
            "boards": item.get(board_key, 1),
        })
    result = list(buckets.values())
    for bucket in result:
        bucket["stocks"].sort(key=lambda s: -s["boards"])
        bucket["max_boards"] = max(s["boards"] for s in bucket["stocks"])
        bucket["ratio"] = 0.0  # 由调用方按总数回填
    result.sort(key=lambda b: (-b["count"], -b["max_boards"]))
    return result


def sector_limit_stats(day: date_cls) -> dict:
    """涨停股票按所属板块聚合——回答「涨停都集中在哪些板块」+ 行业板块行情 + 风口板块"""
    ups = limit_up_list(day)["list"]

    up_sectors = _group_by_sector(ups, "boards")
    for bucket in up_sectors:
        bucket["ratio"] = round(bucket["count"] / len(ups) * 100, 1) if ups else 0.0

    wind = _wind_list() if day == date_cls.today() else []
    return {
        "trade_date": _dash_day(day),
        "up": {"total": len(ups), "sectors": up_sectors},
        "wind": [{"plate_code": w.get("plate_code"), "plate_name": w.get("plate_name"),
                  "catalyst": w.get("catalyst")} for w in wind],
        "industry": [{
            "name": str(s.get("secu_name", "")),
            "change_pct": _ratio_to_pct(s.get("change")),
            "main_fund_diff": _num(s.get("main_fund_diff")),
            "limit_up_count": int(_num(s.get("limit_up_num"), 0)),
            "up_count": int(_num(s.get("limit_up"), 0)),
            "leader": (s.get("first_stock") or {}).get("secu_name", ""),
        } for s in _sector_industry()],
    }


def sector_heat() -> list:
    return [{
        "plate_code": x.get("plate_code"),
        "plate_name": x.get("plate_name"),
        "rank": int(_num(x.get("rank"), 0)),
        "cur_heat": round(_num(x.get("cur_heat")), 1),
        "rank_change": int(_num(x.get("rank_change"), 0)),
        "is_new": int(_num(x.get("is_new"), 0)),
    } for x in _sector_heat()]


def sector_rotation(days: int) -> list:
    raw = lk.get_sector_rotation(days=max(1, min(int(days), 10)))
    return [{
        "trade_date": day.get("trade_date"),
        "plates": [{
            "plate_code": p.get("plate_code"),
            "plate_name": p.get("plate_name"),
            "change_pct": round(_num(p.get("change")), 2),
        } for p in (day.get("plates") or [])],
    } for day in raw]


# ---------------------------------------------------------------------------
# 大盘总览
# ---------------------------------------------------------------------------

@_cached(5)
def _index_snapshot_tx() -> list:
    """腾讯 qt.gtimg.cn 实时快照：三个大盘指数一次请求取齐（GBK，~ 分隔）。
    ⚠️ 东财 market_index_em 对脚本高频请求经常直接掐连接（RemoteDisconnected，盘中
    10s 轮询必挂），故弃用改腾讯源——与分时 mkline 同族，实测稳定。
    字段：[1]名称 [3]现价 [31]涨跌额 [32]涨跌% [37]成交额(万元)。"""
    codes = ",".join(m["tx"] for m in INDEX_META.values())
    resp = requests.get("https://qt.gtimg.cn/q=" + codes,
                        headers={"User-Agent": _BROWSER_UA}, timeout=10)
    resp.raise_for_status()
    out = []
    for line in resp.content.decode("gbk", errors="replace").split(";"):
        line = line.strip()
        if "=" not in line:
            continue
        vkey, _, val = line.partition("=")
        parts = val.strip().strip('"').split("~")
        if len(parts) < 38:
            continue
        sym = vkey.strip().split("_", 1)[-1]          # v_sh000001 → sh000001
        meta = next(({"code": k, "name": m["name"]}
                     for k, m in INDEX_META.items() if m["tx"] == sym), None)
        if not meta:
            continue
        out.append({"code": meta["code"], "name": meta["name"],
                    "price": _num(parts[3]), "change_amt": _num(parts[31]),
                    "change_pct": _num(parts[32]),
                    "amount": _num(parts[37]) * 1e4})  # 万元 → 元，与原口径一致
    return out


def market_overview(day: date_cls) -> dict:
    meta = trade_meta()
    is_today = day == date_cls.today()
    errors = []

    try:
        kph = lk.market_emotion_kph(date=_dash_day(day))
    except Exception as exc:  # noqa: BLE001
        kph = {}
        errors.append(f"市场情绪(开盘红)：{exc}")

    indices = []
    sentiment = {}
    if is_today:
        try:
            indices = _index_snapshot_tx()
        except Exception as exc:  # noqa: BLE001
            errors.append(f"大盘指数：{exc}")
        try:
            raw = lk.market_emotion_cls()
            sentiment = {
                "market_degree": int(_num(raw.get("market_degree"), 0)),
                "turnover": str(raw.get("shsz_balance", "")),
                "turnover_change": str(raw.get("shsz_balance_change_px", "")),
                "up_ratio": str(raw.get("up_ratio", "")),
                "up_ratio_num": int(_num(raw.get("up_ratio_num"), 0)),
                "broken_num": int(_num(raw.get("up_open_num"), 0)),
                "open_ratio": str(raw.get("up_open_ratio", "")),
                "profit_ratio": str(raw.get("profit_ratio", "")),
                "performance": str(raw.get("performance", "")),
                "limit_up_board": raw.get("limit_up_board", {}),
            }
        except Exception as exc:  # noqa: BLE001
            errors.append(f"市场情绪(财联社)：{exc}")

    ups = limit_up_list(day)["list"]
    levels: dict = {}
    for item in ups:
        levels.setdefault(item["boards"], []).append({
            "code": item["code"], "name": item["name"], "sector": item["sector"],
        })
    ladder = [
        {"boards": boards, "count": len(stocks),
         "stocks": sorted(stocks, key=lambda s: s["name"])}
        for boards, stocks in sorted(levels.items(), reverse=True)
    ]

    up_sectors = _group_by_sector(ups, "boards")[:10]

    return {
        "trade_date": _dash_day(day),
        "is_today": is_today,
        "is_trade_day": meta["is_trade_day"],
        "market_status": _market_status(meta["is_trade_day"]),
        "limit_up_count": len(ups),
        "highest_boards": max((s["boards"] for s in ups), default=0),
        "emotion": kph,
        "sentiment": sentiment,
        "indices": indices,
        "ladder": ladder,
        "top_sectors": {
            "up": [{"name": b["name"], "count": b["count"], "max_boards": b["max_boards"]}
                   for b in up_sectors],
        },
        "errors": errors,
    }


# ---------------------------------------------------------------------------
# 涨跌归因
# ---------------------------------------------------------------------------

def _news_brief(day: date_cls, limit: int = 30) -> list:
    try:
        items = _news("important", day)
    except Exception:
        return []
    return [{
        "time": str(x.get("time", ""))[11:16],
        "title": str(x.get("title", "")),
        "content": str(x.get("content", ""))[:160],
    } for x in items[:limit]]


def _build_factor_stats(records: list) -> list:
    """
    records: [{code,name,sector,text}]
    按 FACTORS 归类并统计，最后追加未归类项
    """
    buckets = {f["code"]: {**f, "count": 0, "stocks": [], "sectors": {},
                           "hits": []} for f in FACTORS}
    uncovered = {"code": "unknown", "name": "未归类", "color": "#cbd5e1",
                 "count": 0, "stocks": [], "sectors": {}, "hits": [],
                 "keywords": []}

    for record in records:
        text = record.get("text", "")
        hits = _match_factors(text)
        if not hits:
            uncovered["count"] += 1
            if len(uncovered["stocks"]) < 30:
                uncovered["stocks"].append({
                    "code": record["code"], "name": record["name"], "sector": record["sector"]})
            continue
        for code in hits:
            bucket = buckets[code]
            bucket["count"] += 1
            if len(bucket["stocks"]) < 30:
                bucket["stocks"].append({
                    "code": record["code"], "name": record["name"], "sector": record["sector"]})
            bucket["sectors"][record["sector"]] = bucket["sectors"].get(record["sector"], 0) + 1
            for keyword in bucket["keywords"]:
                if keyword in text and keyword not in bucket["hits"]:
                    bucket["hits"].append(keyword)

    total = len(records) or 1
    result = []
    for bucket in list(buckets.values()) + [uncovered]:
        if bucket["count"] == 0:
            continue
        result.append({
            "factor_code": bucket["code"],
            "factor_name": bucket["name"],
            "color": bucket["color"],
            "count": bucket["count"],
            "ratio": round(bucket["count"] / total * 100, 1),
            "keywords": bucket["hits"][:8],
            "sectors": sorted(
                [{"name": k, "count": v} for k, v in bucket["sectors"].items()],
                key=lambda s: -s["count"])[:8],
            "stocks": bucket["stocks"][:20],
        })
    result.sort(key=lambda f: -f["count"])
    return result


def reason_analyze(day: date_cls) -> dict:
    is_today = day == date_cls.today()
    errors = []

    pool = limit_up_list(day)["list"]
    records = [{
        "code": s["code"], "name": s["name"], "sector": s["sector"],
        "text": f"{s.get('reason', '')} {s['sector']}",
    } for s in pool]
    covered = sum(1 for r in records if r["text"].strip())
    factors = _build_factor_stats(records)
    wind = [{"plate_name": w.get("plate_name"), "catalyst": w.get("catalyst")}
            for w in (_wind_list() if is_today else [])]
    reasons = limit_up_reason(day)

    try:
        news_brief = _news_brief(day)
    except Exception as exc:  # noqa: BLE001
        news_brief = []
        errors.append(f"电报：{exc}")

    try:
        raw_emotion = lk.market_emotion_kph(date=_dash_day(day))
        emotion = {
            "zt": raw_emotion.get("zt"), "sjzt": raw_emotion.get("sjzt"),
            "rise_num": raw_emotion.get("rise_num"), "fall_num": raw_emotion.get("fall_num"),
            "flat": raw_emotion.get("flat"), "sign": raw_emotion.get("sign"),
        }
    except Exception as exc:  # noqa: BLE001
        emotion = {}
        errors.append(f"市场情绪(开盘红)：{exc}")

    return {
        "trade_date": _dash_day(day),
        "total": len(pool),
        "covered": covered,
        "factors": factors,
        "sectors": _group_by_sector(pool, "boards"),
        "wind": wind,
        "reasons": reasons["list"],
        "reasons_notice": reasons.get("notice", ""),
        "news": news_brief,
        "emotion": emotion,
        "errors": errors,
    }


# ---------------------------------------------------------------------------
# 复盘：涨停天梯 / 盘面梳理 / 历史涨停复盘
# ---------------------------------------------------------------------------

def review_ladder(day: date_cls) -> dict:
    raw = lk.get_zttt(date=_dash_day(day))
    rows = raw.get("StockList") or []
    stocks = []
    for row in rows:
        if not isinstance(row, (list, tuple)) or len(row) < 11:
            continue
        stocks.append({
            "code": str(row[0]),
            "name": str(row[1]),
            "boards": int(_num(row[2], 1)) or 1,
            "limit_time": _fmt_epoch(row[3]),
            "sector_code": str(row[4]),
            "sector_name": str(row[5]),
            "one_word": bool(_num(row[6], 0)),
            "popular": bool(_num(row[7], 0)),
            "sector_zt_count": int(_num(row[8], 0)),
            "amount": _num(row[9]),
            "sector_amount": _num(row[10]),
        })
    stocks.sort(key=lambda s: (-s["boards"], s["limit_time"] or "99:99:99"))
    levels: dict = {}
    for stock in stocks:
        levels.setdefault(stock["boards"], []).append(stock)
    return {
        "trade_date": _dash_day(day),
        "total": len(stocks),
        "max_boards": max((s["boards"] for s in stocks), default=0),
        "levels": [{"boards": boards, "count": len(items), "stocks": items}
                   for boards, items in sorted(levels.items(), reverse=True)],
        "stocks": stocks,
    }


def review_pmsl(day: date_cls, size: int) -> dict:
    raw = lk.get_pmsl(date=_dash_day(day), st=max(1, min(int(size), 100)))
    events = []
    for item in raw.get("List") or []:
        events.append({
            "time": _fmt_epoch(item.get("TimeMin")),
            "tag": str(item.get("TagName", "")),
            "tone": int(_num(item.get("TagShuXing"), 1)),
            "sector": str(item.get("ZSName", "")),
            "detail": str(item.get("Detail", "")),
            "stocks": [{"code": str(s[0]), "name": str(s[1])}
                       for s in (item.get("StockList") or []) if len(s) >= 2],
        })
    events.sort(key=lambda e: e["time"])
    return {"trade_date": _dash_day(day), "total": len(events), "list": events}


def review_history(day: date_cls, size: int) -> dict:
    raw = lk.get_his_limit_resumption(date=_dash_day(day), st=max(1, min(int(size), 300)))
    groups = []
    for group in raw.get("list") or []:
        stocks = []
        for row in group.get("StockList") or []:
            if not isinstance(row, (list, tuple)) or len(row) < 12:
                continue
            stocks.append({
                "code": str(row[0]),
                "name": str(row[1]),
                "board_desc": str(row[9]) if len(row) > 9 else "",
                "boards": int(_num(row[10], 1)) if len(row) > 10 else 1,
                "themes": str(row[11]) if len(row) > 11 else "",
                "reason_short": str(row[16]) if len(row) > 16 else "",
                "reason_detail": str(row[17]) if len(row) > 17 else "",
            })
        groups.append({
            "sector_code": str(group.get("ZSCode", "")),
            "sector_name": str(group.get("ZSName", "")),
            "count": len(stocks),
            "stocks": stocks,
        })
    groups.sort(key=lambda g: -g["count"])
    return {
        "trade_date": _dash_day(day),
        "nums": raw.get("nums") or {},
        "groups": groups,
    }


# ---------------------------------------------------------------------------
# 个股（人气榜 / K 线）
# ---------------------------------------------------------------------------

def news_telegraph(day: date_cls, category: str, limit: int) -> dict:
    """财联社电报快讯（支持历史日期）"""
    items = _news(category, day)
    return {
        "trade_date": _dash_day(day),
        "total": len(items),
        "list": [{
            "time": str(x.get("time", "")),
            "title": str(x.get("title", "")),
            "content": str(x.get("content", "")),
        } for x in items[:max(1, int(limit))]],
    }


def stock_hot(limit: int) -> dict:
    raw = lk.stock_hot_rank_ths(limit=max(1, min(int(limit), 100)))
    items = [{
        "rank": int(_num(x.get("rank"), 0)),
        "code": str(x.get("code", "")),
        "name": str(x.get("name", "")),
        "price": _num(x.get("price")),
        "change_pct": round(_num(x.get("change_pct")), 2),
        "tags": (x.get("tag") or {}).get("concept_tag") or [],
        "popularity": (x.get("tag") or {}).get("popularity_tag", ""),
    } for x in raw]
    return {"total": len(items), "list": items}


# ---------------------------------------------------------------------------
# 个股：搜索（腾讯 smartbox）/ 分钟线（腾讯 mkline，与 Ashare 同源）/ K 线（levistock 财联社）
# ---------------------------------------------------------------------------

def _smartbox(q: str) -> list:
    """腾讯 smartbox 联想搜索原始行（支持代码/名称/拼音）：
    返回 [{market, code, name, pinyin, type}]，type ∈ GP-A/GP/ZS/ETF/LOF/KJ…"""
    q = str(q or "").strip()
    if not q:
        return []
    resp = requests.get(
        "https://smartbox.gtimg.cn/s3/",
        params={"v": 2, "q": q, "t": "all"},
        headers={"User-Agent": _BROWSER_UA},
        timeout=10,
    )
    resp.raise_for_status()
    raw = resp.content.decode("gbk", "replace")
    if '="' not in raw:
        return []
    inner = raw.split('="', 1)[1].rsplit('"', 1)[0]
    # 中文以 \uXXXX 转义出现，latin1+unicode_escape 还原（探针验证）
    text = inner.encode("latin1", "backslashreplace").decode("unicode_escape")
    items = []
    for row in text.split("^"):
        parts = row.split("~")
        if len(parts) < 5:
            continue
        items.append({
            "market": parts[0], "code": parts[1], "name": parts[2],
            "pinyin": parts[3], "type": parts[4],
        })
    return items


def stock_search(q: str, limit: int = 8) -> dict:
    """个股搜索：只留 A 股个股（GP-A / GP，sh/sz/bj），供前端下拉联想"""
    items = [x for x in _smartbox(q)
             if x["type"] in ("GP-A", "GP") and x["market"] in ("sh", "sz", "bj")]
    items = items[:max(1, min(int(limit), 20))]
    return {"total": len(items), "list": items}


def _stock_symbol(code: str) -> str:
    """6 位 A 股代码 → 腾讯符号（sh/sz/bj 前缀），判不出抛 ValueError"""
    c = re.sub(r"\D", "", str(code or ""))
    if len(c) != 6:
        raise ValueError("非法股票代码：%s" % code)
    if c.startswith(("60", "68", "51", "56", "58")):
        return "sh" + c
    if c.startswith(("00", "30", "02", "12", "15", "16", "18")):
        return "sz" + c
    if c.startswith(("43", "83", "87", "88", "92")):
        return "bj" + c
    raise ValueError("不支持的股票代码：%s" % code)


@_cached(15)
def _minute_tx(symbol: str, count: int = 242, end_date: str = "") -> dict:
    """腾讯 1 分钟线（与 Ashare get_price_min_tx 同源同 URL）。
    end_date(YYYY-MM-DD) 非空 = 历史分时（上游返回截至该日的 count 根，含前日尾段）。
    返回 {points, price, prev_close, name, date}：实时模式响应内嵌 qt 节点
    （[1]=名称、[3]=现价、[4]=昨收），一次请求取齐分时与昨收参考；
    历史模式 qt 是实时快照无意义 ⇒ price=该日收盘（末点），prev_close=None 由调用方补。
    ⚠️ 时间格式两态：指数 "2026-10-09 14:52:00"、个股 "202610091456"，须兼容解析。"""
    symbol = str(symbol or "").strip()
    resp = requests.get(
        "https://ifzq.gtimg.cn/appstock/app/kline/mkline",
        params={"param": "%s,m1,,%s,%d" % (symbol, str(end_date).strip(),
                                           max(10, min(int(count), 800)))},
        headers={"User-Agent": _BROWSER_UA},
        timeout=15,
    )
    resp.raise_for_status()
    node = (resp.json().get("data") or {}).get(symbol) or {}
    rows = node.get("m1") or []
    qt = ((node.get("qt") or {}).get(symbol)) or []
    history = bool(str(end_date).strip())

    def _dt(raw):
        s = str(raw)
        if " " in s:
            return s.split(" ")[0], s.split(" ")[1][:5]
        if len(s) >= 12:
            return "%s-%s-%s" % (s[:4], s[4:6], s[6:8]), "%s:%s" % (s[8:10], s[10:12])
        return s, ""

    points = []
    if rows:
        # 末行日期 = 最近交易日；首部可能带前一交易日边界 bar，按日期过滤掉
        last_day = _dt(rows[-1][0])[0]
        # 均价线：分钟成交量加权的累计均价（量纲无关，分子分母同单位）
        csum_pv = csum_v = 0.0
        for row in rows:
            if len(row) < 6:
                continue
            day, hhmm = _dt(row[0])
            if day != last_day:
                continue
            close = _num(row[2])
            vol = _num(row[5])
            csum_pv += close * vol
            csum_v += vol
            points.append({"t": hhmm, "price": round(close, 2),
                           "avg": round(csum_pv / csum_v, 2) if csum_v > 0 else round(close, 2)})
    last_close = points[-1]["price"] if points else None
    return {
        "points": points,
        "price": last_close if history else (_num(qt[3]) if len(qt) > 3 else None),
        "prev_close": None if history else (_num(qt[4]) if len(qt) > 4 else None),
        "name": str(qt[1]) if len(qt) > 1 else "",
        "date": _dt(rows[-1][0])[0] if rows else "",
    }


@_cached(3600)
def _stock_sector_index(code: str):
    """个股 → 所属东财行业板块（levistock sector_stock_belong_em）→ 板块指数腾讯符号。
    板块名去掉罗马数字后缀（白酒Ⅱ→白酒）后用 smartbox 搜第一个 sh/sz 的 ZS 指数；
    找不到对应指数返回 None（板块线优雅缺席）。"""
    try:
        belongs = lk.sector_stock_belong_em([re.sub(r"\D", "", str(code))]) or []
    except Exception:  # noqa: BLE001  归属查询失败不致命
        return None
    sector = str((belongs[0] if belongs else {}).get("sector_name") or "").strip()
    if not sector:
        return None
    kw = re.sub(r"[ⅠⅡⅢⅣⅤ0-9]+$", "", sector).strip() or sector
    for item in _smartbox(kw):
        if item["type"] == "ZS" and item["market"] in ("sh", "sz"):
            return {"sector_name": sector, "index_name": item["name"],
                    "symbol": item["market"] + item["code"]}
    return None


def stock_minute(code: str, date: str = "") -> dict:
    """个股分时：分钟点 + 现价/昨收（qt）+ 板块指数叠加线（板块分时同源取）。
    date(YYYY-MM-DD) 非空 = 历史分时：现价=该日收盘、昨收=前一交易日收盘，
    从财联社日 K 全量缓存定位；板块昨收从腾讯日 K 切片定位。"""
    symbol = _stock_symbol(code)
    day = str(date or "").strip()
    base = _minute_tx(symbol, end_date=day)

    prev_close = price = None
    if day:
        full = _stock_kline_cls_cached(re.sub(r"\D", "", str(code)), _STOCK_PERIODS["day"])
        plain = day.replace("-", "")
        idx = next((i for i, b in enumerate(full) if str(b.get("date", "")) == plain), -1)
        if idx < 0:
            raise ValueError("暂无 %s 的分时数据（非交易日或超出缓存范围）" % day)
        prev_close = _num(full[idx - 1].get("preclose_px")) or \
            (_num(full[idx - 1].get("close_px")) if idx >= 1 else None)
        price = _num(full[idx].get("close_px"))

    board = None
    sec = _stock_sector_index(code)
    if sec:
        try:
            bmin = _minute_tx(sec["symbol"], end_date=day)
            if bmin["points"]:
                b_prev = bmin["prev_close"]
                if day:
                    # 历史模式：板块昨收=腾讯日 K 中该日前一交易日收盘
                    dbars = _index_kline_tx(sec["symbol"], 320)
                    bidx = next((i for i, b in enumerate(dbars) if b["date"] == day), -1)
                    b_prev = dbars[bidx - 1]["close"] if bidx >= 1 else None
                board = {**sec, "points": bmin["points"], "prev_close": b_prev}
        except Exception:  # noqa: BLE001  板块线失败不影响个股分时
            board = None
    points = base["points"]
    return {
        "code": re.sub(r"\D", "", str(code)),
        "name": base["name"] or str(code),
        "symbol": symbol,
        "date": base["date"],
        "price": price if day else base["price"],
        "prev_close": prev_close if day else base["prev_close"],
        "board": board,
        "total": len(points),
        "list": points,
    }


_STOCK_PERIODS = {"day": "daily", "week": "weekly", "month": "monthly", "year": "yearly"}


@_cached(60)
def _stock_kline_cls_cached(code: str, ktype: str) -> list:
    """财联社周期 K 全量缓存（2500 根 ≈ 10 年），stock_kline 按 limit/end 切片"""
    return lk.stock_kline_cls(code, kline_type=ktype, limit=2500) or []


def stock_kline(code: str, period: str = "day", limit: int = 120,
                end: str = "") -> dict:
    """个股周期 K（levistock stock_kline_cls / 财联社源）。
    动态分页：limit 取末段根数，end(YYYY-MM-DD) 为游标（取该日及更早），has_more 指示前端可继续左翻。
    上游字段：date=20261009、open_px/close_px/high_px/low_px、
    business_amount(股)/business_balance(元)、change=比率、preclose_px。"""
    p = str(period or "day").strip().lower()
    if p not in _STOCK_PERIODS:
        raise ValueError("不支持的周期：%s（可用：%s）" % (period, " / ".join(_STOCK_PERIODS)))
    raw = _stock_kline_cls_cached(re.sub(r"\D", "", str(code)), _STOCK_PERIODS[p])
    bars = []
    for x in raw:
        d = str(x.get("date", ""))
        bars.append({
            "date": "%s-%s-%s" % (d[:4], d[4:6], d[6:8]) if len(d) == 8 else d,
            "open": _num(x.get("open_px")),
            "close": _num(x.get("close_px")),
            "high": _num(x.get("high_px")),
            "low": _num(x.get("low_px")),
            "volume": int(_num(x.get("business_amount")) / 100),  # 股 → 手，与指数口径一致
            "amount": _num(x.get("business_balance")),            # 元
            "change_pct": round(_num(x.get("change")) * 100, 2),  # 比率 → %
        })
    if end:
        bars = [b for b in bars if b["date"] <= str(end).strip()]
    n = max(10, min(int(limit), 2500))
    page = bars[-n:]
    return {
        "code": re.sub(r"\D", "", str(code)),
        "period": p,
        "trade_date": page[-1]["date"] if page else "",
        "total": len(page),
        "has_more": len(bars) > len(page),
        "list": page,
    }


# ---------------------------------------------------------------------------
# 指数（大盘周期 K 线 + 分时）：
#   周期 K（day/week/month/year + 成交额）= 雪球 kline.json（免费、begin=now 末根实时、
#     year 原生支持；腾讯 fqkline 的 year 只返回当年 1 根、全系列无成交额 ⇒ 弃用其周期源）。
#   分时 = Ashare（腾讯 1 分钟线，lib/Ashare.py），昨收参考复用腾讯日 K。
#   zzshare 对指数只有 rt_k 快照可用（kline 引擎错解指数码），周期源切换后已不使用。
#   东财 push2his 对脚本请求经常直接掐连接（root 可达但 API 被拒），弃用。
# ---------------------------------------------------------------------------

INDEX_META = {
    "000001.SH": {"tx": "sh000001", "xq": "SH000001", "name": "上证指数"},
    "399006.SZ": {"tx": "sz399006", "xq": "SZ399006", "name": "创业板指"},
    "000688.SH": {"tx": "sh000688", "xq": "SH000688", "name": "科创50"},
}

# 周期 K 线：period → (全量缓存根数, 缓存 TTL 秒)。
# 全量缓存后由 index_kline/stock_kline 按 limit/end 切片（前端动态分页加载），
# 根数取 2500（雪球/财联社均实测可回溯约 10 年）。day 短 TTL 保盘中实时（雪球末根本身就是实时 bar）。
_KLINE_PERIODS = {
    "day": (2500, 60),
    "week": (2500, 300),
    "month": (2500, 300),
    "year": (2500, 300),
}

_BROWSER_UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
               "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")

_XQ_SESSION = None


def _xq_session():
    """雪球会话单例：kline 接口要求先访问页面拿 cookie；失效时由 _index_kline_xq 重建重试"""
    global _XQ_SESSION
    if _XQ_SESSION is None:
        s = requests.Session()
        s.headers.update({"User-Agent": _BROWSER_UA, "Referer": "https://xueqiu.com/hq"})
        s.get("https://xueqiu.com/hq", timeout=10)
        _XQ_SESSION = s
    return _XQ_SESSION


def _xq_reset():
    global _XQ_SESSION
    _XQ_SESSION = None


@_cached(300)
def _index_kline_tx(tx_code: str, limit: int) -> list:
    """腾讯指数日 K：行 = [日期, 开, 收, 高, 低, 量(手)]，升序（仅剩分钟线昨收参考在用）"""
    resp = requests.get(
        "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get",
        params={"param": "%s,day,,,%d,qfq" % (tx_code, max(10, min(int(limit), 500)))},
        headers={"User-Agent": _BROWSER_UA},
        timeout=15,
    )
    resp.raise_for_status()
    node = (resp.json().get("data") or {}).get(tx_code) or {}
    rows = node.get("qfqday") or node.get("day") or []
    bars = []
    prev_close = None
    for row in rows:
        if len(row) < 6:
            continue
        close = _num(row[2])
        bars.append({
            "date": str(row[0]),
            "open": _num(row[1]), "close": close,
            "high": _num(row[3]), "low": _num(row[4]),
            "volume": _num(row[5]),
            "change_pct": round((close / prev_close - 1) * 100, 2)
                          if prev_close else 0.0,
        })
        prev_close = close
    return bars


@_cached(120)
def _index_kline_xq_cached(xq_symbol: str, period: str, count: int) -> list:
    """雪球周期 K 线（唯一实现，period 白名单见 _KLINE_PERIODS）：
    行字段 timestamp/volume(股)/open/high/low/close/chg/percent/turnoverrate/amount(元)。
    begin=当前时刻 ⇒ 末根为当日实时 bar（day 周期盘中即实时，无需再打快照）。"""
    url = "https://stock.xueqiu.com/v5/stock/chart/kline.json"
    params = {"symbol": xq_symbol, "begin": int(time.time() * 1000), "period": period,
              "type": "before", "count": str(-int(count)), "indicator": "kline"}
    cols, items = [], []
    for _ in (0, 1):  # cookie 失效自愈：重置会话重取一次
        try:
            resp = _xq_session().get(url, params=params, timeout=15)
            resp.raise_for_status()
            data = (resp.json().get("data") or {})
            cols = data.get("column") or []
            items = data.get("item") or []
            if cols and items:
                break
        except Exception:  # noqa: BLE001  网络抖动/cookie 失效都走重建
            pass
        _xq_reset()
    if not cols or not items:
        return []
    idx = {name: i for i, name in enumerate(cols)}
    # 涨跌幅优先用雪球原生 percent（相对各根自身昨收，end 游标切片后依然正确）
    has_pct = "percent" in idx
    bars = []
    prev_close = None
    for row in items:
        close = round(_num(row[idx["close"]]), 2)
        if has_pct:
            pct = round(_num(row[idx["percent"]]), 2)
        else:
            pct = round((close / prev_close - 1) * 100, 2) if prev_close else 0.0
        bars.append({
            "date": datetime.fromtimestamp(_num(row[idx["timestamp"]]) / 1000).strftime("%Y-%m-%d"),
            "open": round(_num(row[idx["open"]]), 2),
            "close": close,
            "high": round(_num(row[idx["high"]]), 2),
            "low": round(_num(row[idx["low"]]), 2),
            "volume": int(_num(row[idx["volume"]]) / 100),  # 股 → 手，与腾讯口径一致
            "amount": round(_num(row[idx["amount"]]), 0),   # 元
            "change_pct": pct,
        })
        prev_close = close
    return bars


def index_kline(code: str, period: str = "day", limit: int = 120,
                end: str = "") -> dict:
    """指数周期 K。动态分页：limit 取末段根数，end(YYYY-MM-DD) 为游标（取该日及更早），
    has_more 指示前端可继续左翻；全量来自雪球缓存切片。"""
    meta = INDEX_META.get(str(code or "").strip())
    if meta is None:
        raise ValueError("不支持的指数代码：%s（可用：%s）" % (code, " / ".join(INDEX_META)))
    period = str(period or "day").strip().lower()
    if period not in _KLINE_PERIODS:
        raise ValueError("不支持的周期：%s（可用：%s）" % (period, " / ".join(_KLINE_PERIODS)))
    count, _ttl = _KLINE_PERIODS[period]
    full = _index_kline_xq_cached(meta["xq"], period, count)
    if end:
        e = str(end).strip()
        full = [b for b in full if b["date"] <= e]
    n = max(10, min(int(limit), 2500))
    page = full[-n:]
    return {
        "code": code,
        "name": meta["name"],
        "period": period,
        "trade_date": page[-1]["date"] if page else "",
        "total": len(page),
        "has_more": len(full) > len(page),
        "list": page,
    }


def _ashare():
    """Ashare（lib/Ashare.py，腾讯免费行情）延迟导入。指数分时的唯一免费来源：
    zzshare stk_mins 会把指数码错解成个股，levistock 只有指数实时快照。

    lib 目录可用环境变量 STOCK_LIB_DIR 覆盖（由启动器注入）；缺省 = 本文件同级的 lib/。"""
    import sys

    lib_dir = os.environ.get("STOCK_LIB_DIR", "").strip()
    if not lib_dir:
        lib_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "lib")
    if lib_dir not in sys.path:
        sys.path.insert(0, lib_dir)
    import Ashare

    return Ashare


@_cached(15)
def _index_minute_ashare(tx_code: str, end_date: str = "") -> list:
    """Ashare 指数 1 分钟线：最近 242 根 ≈ 最近一个交易日全天。
    end_date(YYYY-MM-DD) 非空 = 该交易日分时。
    Ashare 返回首行可能是前一交易日的 15:00 边界 bar，按最后一行的日期过滤掉。"""
    df = _ashare().get_price(tx_code, end_date=str(end_date or ""), frequency="1m", count=242)
    if df is None or df.empty:
        return []
    day = df.index[-1].strftime("%Y-%m-%d")
    out = []
    csum_pv = csum_v = 0.0   # 均价线：分钟量加权累计均价（个股/指数分时同一口径）
    for ts, row in df[df.index.strftime("%Y-%m-%d") == day].iterrows():
        price = round(float(row["close"]), 2)
        vol = float(row["volume"])
        csum_pv += price * vol
        csum_v += vol
        out.append({"t": ts.strftime("%H:%M"), "price": price,
                    "avg": round(csum_pv / csum_v, 2) if csum_v > 0 else price})
    return out


def index_minute(code: str, date: str = "") -> dict:
    """指数分时。date(YYYY-MM-DD) 非空 = 历史分时：昨收/收盘从雪球日 K 全量缓存定位。"""
    meta = INDEX_META.get(str(code or "").strip())
    if meta is None:
        raise ValueError("不支持的指数代码：%s（可用：%s）" % (code, " / ".join(INDEX_META)))
    day = str(date or "").strip()
    points = _index_minute_ashare(meta["tx"], day)

    if day:
        # 历史分时：昨收=前一交易日收盘、收盘价=当日分时末点（雪球全量日 K 缓存切片定位）
        full = _index_kline_xq_cached(meta["xq"], "day", _KLINE_PERIODS["day"][0])
        idx = next((i for i, b in enumerate(full) if b["date"] == day), -1)
        if idx < 0:
            raise ValueError("暂无 %s 的分时数据（非交易日或超出缓存范围）" % day)
        return {
            "code": code,
            "name": meta["name"],
            "trade_date": day,
            "prev_close": full[idx - 1]["close"] if idx >= 1 else None,
            "price": full[idx]["close"],
            "total": len(points),
            "list": points,
        }

    # 昨收参考线：日 K 倒数第二根收盘（分钟线接口不带昨收，复用已缓存的日 K 一次取齐）
    bars = _index_kline_tx(meta["tx"], 3)
    prev_close = bars[-2]["close"] if len(bars) >= 2 else None
    return {
        "code": code,
        "name": meta["name"],
        "trade_date": bars[-1]["date"] if bars else "",
        "prev_close": prev_close,
        "total": len(points),
        "list": points,
    }
