#!/usr/bin/python
# -*- coding: UTF-8 -*-

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.edge.options import Options
from selenium.webdriver.support.ui import Select
import time
import pandas as pd
import random


class SeleniumAutoSubmit:
    def __init__(self, url, keep_browser_open=True, use_debug_mode=True, debugger_address="localhost:9222", human_like=True, use_cursor=True):
        self.url = url
        self.keep_browser_open = keep_browser_open
        self.human_like = human_like
        self.use_cursor = use_cursor

        if use_debug_mode:
            print(f"🔗 正在连接到调试模式浏览器: {debugger_address}")
            edge_options = Options()
            edge_options.add_experimental_option("debuggerAddress", debugger_address)
            self.driver = webdriver.Edge(options=edge_options)
            print("✅ 已连接到现有浏览器窗口")
        else:
            print("🌐 正在打开新浏览器窗口")
            self.driver = webdriver.Edge()
            print("✅ 新浏览器已打开")

        self.wait = WebDriverWait(self.driver, 10)

    # ──────────────────────────────────────────────
    #  基础工具
    # ──────────────────────────────────────────────

    def human_delay(self, min_sec=0.3, max_sec=0.8):
        """模拟人工延迟"""
        time.sleep(random.uniform(min_sec, max_sec))

    def move_to(self, element, pre_delay=(0.1, 0.3), post_delay=(0.15, 0.35)):
        """
        将鼠标移动到目标元素。
        use_cursor=True  → 通过 ActionChains 真实移动光标
        use_cursor=False → 跳过移动，仅保留时间间隔
        """
        if self.use_cursor:
            ActionChains(self.driver).move_to_element(element).perform()
        if pre_delay:
            self.human_delay(*pre_delay)
        if post_delay:
            self.human_delay(*post_delay)

    def blur_focus(self):
        """
        点击页面空白处，让当前聚焦元素失焦。
        用于下拉框选完后确保其视觉上完全关闭。
        """
        try:
            self.driver.execute_script("document.activeElement.blur();")
        except Exception:
            pass
        self.human_delay(0.15, 0.3)

    # ──────────────────────────────────────────────
    #  输入框：逐字输入
    # ──────────────────────────────────────────────

    def fill_input(self, by, value, content):
        """填充输入框（human_like=True 时逐字符打入，并移动光标聚焦）"""
        try:
            element = self.wait.until(EC.presence_of_element_located((by, value)))

            self.move_to(element, pre_delay=(0.2, 0.4), post_delay=None)
            element.click()
            self.human_delay(0.1, 0.25)
            element.clear()

            if self.human_like:
                for char in content:
                    element.send_keys(char)
                    delay = random.uniform(0.06, 0.18) if random.random() > 0.15 else random.uniform(0.25, 0.45)
                    time.sleep(delay)
            else:
                element.send_keys(content)

            self.human_delay(0.2, 0.4)
            print(f"  填充: {value} = {content}")
        except Exception as e:
            print(f"  填充失败: {e}")

    # ──────────────────────────────────────────────
    #  下拉菜单：展开 → 方向键逐项高亮 → 确认 → 关闭
    # ──────────────────────────────────────────────

    def select_dropdown(self, by, value, target_value):
        """
        human_like=True 完整流程：
          1. 光标移到 <select> 并点击展开
          2. 用 ↓/↑ 方向键从当前项逐步移动到目标项
             （每步之间有停顿，产生肉眼可见的逐项高亮效果）
          3. 按 Enter 确认选中，下拉自动收起
          4. 点击页面空白处彻底失焦，确保下拉框视觉关闭

        注意：原生 <select> 的选项是操作系统级 UI，
        ActionChains.move_to_element(<option>) 不会触发高亮，
        键盘导航才是唯一有效的方式。
        """
        try:
            select_el = self.wait.until(EC.presence_of_element_located((by, value)))

            if not self.human_like:
                Select(select_el).select_by_value(target_value)
                return

            # ① 光标移到下拉框
            self.move_to(select_el, pre_delay=(0.2, 0.4), post_delay=(0.1, 0.2))

            # ② 点击展开
            select_el.click()
            self.human_delay(0.3, 0.55)

            # ③ 解析所有选项，定位当前项与目标项的索引
            sel = Select(select_el)
            all_options = sel.options
            option_values = [o.get_attribute("value") for o in all_options]

            current_index = next(
                (i for i, o in enumerate(all_options) if o.is_selected()), 0
            )
            target_index = next(
                (i for i, v in enumerate(option_values) if v == target_value), None
            )

            if target_index is None:
                print(f"  未找到选项 value='{target_value}'，回退到 Select API")
                sel.select_by_value(target_value)
                # 回退路径同样需要关闭下拉框
                select_el.send_keys(Keys.ESCAPE)
                self.blur_focus()
                return

            # ④ 用方向键逐项移动，每步暂停产生高亮滚动效果
            steps = target_index - current_index
            nav_key = Keys.ARROW_DOWN if steps > 0 else Keys.ARROW_UP

            for _ in range(abs(steps)):
                select_el.send_keys(nav_key)
                # 每步停顿 120–300 ms，让高亮效果肉眼可见
                self.human_delay(0.12, 0.30)

            # ⑤ 在目标项上额外停留，模拟"确认阅读"
            self.human_delay(0.2, 0.45)

            # ⑥ 按 Enter 确认选中，下拉框随之关闭
            select_el.send_keys(Keys.RETURN)
            self.human_delay(0.2, 0.35)

            # ⑦ 点击空白处彻底失焦，确保下拉框视觉上完全关闭
            self.blur_focus()

            print(f"  选择: {value} = {target_value}")
        except Exception as e:
            print(f"  下拉选择失败: {e}")

    # ──────────────────────────────────────────────
    #  文件上传：直接赋值，无需真人效果
    # ──────────────────────────────────────────────

    def upload_file(self, by, value, file_path):
        """上传文件（直接 send_keys，跳过人工效果，浏览器自动预览）"""
        try:
            element = self.driver.find_element(by, value)
            element.send_keys(file_path)
            print(f"  上传文件: {file_path}")
        except Exception as e:
            print(f"  上传失败: {e}")

    # ──────────────────────────────────────────────
    #  按钮点击：光标移入 → 悬停 → 点击
    # ──────────────────────────────────────────────

    def click_button(self, by, value):
        """点击按钮（human_like=True 时先移光标、短暂悬停再点击）"""
        try:
            element = self.wait.until(EC.element_to_be_clickable((by, value)))

            if self.human_like:
                self.move_to(element, pre_delay=(0.2, 0.4), post_delay=(0.15, 0.35))
                self.human_delay(0.1, 0.3)

            element.click()
            print(f"  点击: {value}")
            time.sleep(3)
            return True
        except Exception as e:
            print(f"  点击失败: {e}")
            return False

    # ──────────────────────────────────────────────
    #  提交单人
    # ──────────────────────────────────────────────

    def submit_person(self, person_info):
        print("\n  👤 开始录入人员信息...")

        print("  ✏️  正在填写姓名...")
        self.fill_input(By.ID, "name", person_info['name'])

        print("  ✏️  正在选择性别...")
        self.select_dropdown(By.ID, "gender", str(person_info['gender']))
        # select_dropdown 末尾已调用 blur_focus()，下拉框此时已完全关闭

        if person_info.get('photo'):
            print("  📸 正在上传照片...")
            self.upload_file(By.ID, "photoInput", person_info['photo'])

        print("  💾 正在保存...")
        self.click_button(By.CLASS_NAME, "btn-save")

    # ──────────────────────────────────────────────
    #  批量处理
    # ──────────────────────────────────────────────

    def batch_submit_from_excel(self, excel_path):
        df = pd.read_excel(excel_path)
        print(f"📊 读取到 {len(df)} 条Excel数据\n")

        self.driver.get(self.url)
        print("🌐 页面已打开")
        time.sleep(2)

        for index, row in df.iterrows():
            print(f"\n{'=' * 60}")
            print(f"📝 正在录入第 {index + 1}/{len(df)} 人: {row['name']}")
            print(f"{'=' * 60}")

            person_info = {
                'name': row['name'],
                'gender': row['gender'],
                'photo': row['photo']
            }

            self.submit_person(person_info)

            if index < len(df) - 1:
                print("\n  🔄 准备录入下一条...")
                self.driver.refresh()
                time.sleep(2)

        print(f"\n{'=' * 60}")
        print(f"✅ 全部完成！共处理 {len(df)} 人")
        print(f"{'=' * 60}")

    def close(self):
        if self.keep_browser_open:
            print("\n🌐 浏览器保持打开状态，请手动关闭")
        else:
            self.driver.quit()


if __name__ == "__main__":
    url = "F:/Projects/Study/github/Pycharm/personWithGender.html"
    excel_file = "F:/Projects/Study/github/Pycharm/person.xlsx"

    auto = SeleniumAutoSubmit(
        url,
        keep_browser_open=True,
        use_debug_mode=False,
        debugger_address="localhost:9222",
        human_like=True,
        use_cursor=True
    )

    try:
        auto.batch_submit_from_excel(excel_file)
        input("\n按Enter键结束程序...")
    finally:
        auto.close()