"""
股票行情本地服务（端口 7300）

数据源：levistock（东方财富 / 财联社 / 开盘红 / 同花顺 / 问财）
上游均为公网实时接口，本服务只做「取数 + 聚合 + 归因」，不做任何本地模拟数据。

启动：
    tool\\stock-service\\start.bat
或
    .venv\\Scripts\\python.exe -m uvicorn main:app --host 0.0.0.0 --port 7300

前端通过 vite 代理访问：/api-stock/xxx  ->  http://localhost:7300/xxx
"""

import service
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="股票行情本地服务",
    description="基于 levistock 的涨停统计、板块聚合与涨跌归因数据服务",
    version="1.0.0",
)

# 开发期允许任意来源（生产经 nginx / vite 代理同源访问时用不到）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


def _run(fn, *args, **kwargs):
    """统一异常出口：上游网络/接口异常 → 502，参数异常 → 400"""
    try:
        return fn(*args, **kwargs)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=502, detail=f"上游数据源异常：{exc}") from exc


@app.get("/health")
def health():
    return {"ok": True, "service": "stock-service", "version": app.version}


# ------------------------------- 元信息 ------------------------------------

@app.get("/meta/trade")
def meta_trade():
    """交易日元信息：今天是否交易日 + 近 20 个交易日（供日期选择器用）"""
    return _run(service.trade_meta)


# ------------------------------- 大盘总览 ----------------------------------

@app.get("/market/overview")
def market_overview(date: str = Query("", description="交易日 YYYY-MM-DD，默认今天")):
    return _run(service.market_overview, service.parse_date(date))


@app.get("/index/kline")
def index_kline(
    code: str = Query("000001.SH",
                      description="指数代码：000001.SH 上证 / 399006.SZ 创业板指 / 000688.SH 科创50"),
    period: str = Query("day", description="周期：day / week / month / year"),
    limit: int = Query(120, ge=10, le=2500),
    end: str = Query("", description="游标 YYYY-MM-DD：取该日及更早的 limit 根，空=最新段"),
):
    """指数周期 K 线：雪球源（day/week/month/year），动态分页（end 游标 + has_more）"""
    return _run(service.index_kline, code, period, limit, end)


@app.get("/index/minute")
def index_minute(
    code: str = Query("000001.SH",
                      description="指数代码：000001.SH 上证 / 399006.SZ 创业板指 / 000688.SH 科创50"),
    date: str = Query("", description="交易日 YYYY-MM-DD，默认最近一个交易日"),
):
    """指数分时线：Ashare（腾讯 1 分钟线）分钟点 + 昨收，date 支持历史分时"""
    return _run(service.index_minute, code, date)


# ------------------------------- 涨停 -------------------------------

@app.get("/limit/up")
def limit_up(date: str = Query("", description="交易日 YYYY-MM-DD，默认今天")):
    return _run(service.limit_up_list, service.parse_date(date))


@app.get("/limit/up-reason")
def limit_up_reason(date: str = Query("", description="仅支持当日")):
    return _run(service.limit_up_reason, service.parse_date(date))


@app.get("/limit/yesterday")
def limit_yesterday(date: str = Query("", description="交易日 YYYY-MM-DD，默认今天")):
    return _run(service.yesterday_limit_up, service.parse_date(date))


# ------------------------------- 板块 -------------------------------------

@app.get("/sector/limit-stats")
def sector_limit_stats(date: str = Query("", description="交易日 YYYY-MM-DD，默认今天")):
    """涨停股票按所属板块聚合 + 行业板块行情 + 风口板块"""
    return _run(service.sector_limit_stats, service.parse_date(date))


@app.get("/sector/heat")
def sector_heat():
    return _run(service.sector_heat)


@app.get("/sector/rotation")
def sector_rotation(days: int = Query(4, ge=1, le=10)):
    return _run(service.sector_rotation, days)


# ------------------------------- 归因 -------------------------------------

@app.get("/reason/analyze")
def reason_analyze(date: str = Query("", description="交易日 YYYY-MM-DD，默认今天")):
    """按因素（政策/市场/外围/题材/业绩/公告/资金/技术/风险）归类涨停原因"""
    return _run(service.reason_analyze, service.parse_date(date))


@app.get("/news/telegraph")
def news_telegraph(
    date: str = Query("", description="日期 YYYY-MM-DD，默认今天"),
    category: str = Query("important", description="all / important / company"),
    limit: int = Query(40, ge=1, le=200),
):
    return _run(service.news_telegraph, service.parse_date(date), category, limit)


# ------------------------------- 复盘 -------------------------------------

@app.get("/review/ladder")
def review_ladder(date: str = Query("", description="交易日 YYYY-MM-DD，默认今天")):
    """涨停天梯（开盘红，含所属板块、连板数、大单一字/人气标记）"""
    return _run(service.review_ladder, service.parse_date(date))


@app.get("/review/pmsl")
def review_pmsl(
    date: str = Query("", description="交易日 YYYY-MM-DD，默认今天"),
    size: int = Query(50, ge=1, le=100),
):
    """盘面梳理：当日板块/个股事件流（大单一字、直线拉升、人气股杀跌…）"""
    return _run(service.review_pmsl, service.parse_date(date), size)


@app.get("/review/history")
def review_history(
    date: str = Query("", description="交易日 YYYY-MM-DD，默认今天"),
    size: int = Query(100, ge=1, le=300),
):
    """历史涨停复盘：按板块分组，含涨停原因短标签与详细说明（开盘红）"""
    return _run(service.review_history, service.parse_date(date), size)


# ------------------------------- 个股 -------------------------------------

@app.get("/stock/hot")
def stock_hot(limit: int = Query(50, ge=1, le=100)):
    """同花顺人气股排行"""
    return _run(service.stock_hot, limit)


@app.get("/stock/search")
def stock_search(
    q: str = Query(..., min_length=1, description="搜索词：代码 / 名称 / 拼音"),
    limit: int = Query(8, ge=1, le=20),
):
    """个股联想搜索（腾讯 smartbox，只返回 A 股个股）"""
    return _run(service.stock_search, q, limit)


@app.get("/stock/minute")
def stock_minute(
    code: str = Query(..., description="6 位股票代码，如 600519"),
    date: str = Query("", description="交易日 YYYY-MM-DD，默认最近一个交易日"),
):
    """个股分时：分钟点 + 现价/昨收 + 板块指数叠加线（腾讯 mkline 源），date 支持历史分时"""
    return _run(service.stock_minute, code, date)


@app.get("/stock/kline")
def stock_kline(
    code: str = Query(..., description="6 位股票代码，如 600519"),
    period: str = Query("day", description="day / week / month / year"),
    limit: int = Query(120, ge=10, le=2500),
    end: str = Query("", description="游标 YYYY-MM-DD：取该日及更早的 limit 根，空=最新段"),
):
    """个股周期 K 线（levistock/财联社源），动态分页（end 游标 + has_more）"""
    return _run(service.stock_kline, code, period, limit, end)
