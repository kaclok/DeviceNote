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
    def __init__(self, url, keep_browser_open=True, human_like=True, use_debug_mode=True, debugger_address="localhost:9222"):
        self.url = url
        self.keep_browser_open = keep_browser_open
        self.human_like = human_like  # 是否模拟人工操作

        if use_debug_mode:
            # 模式1：连接到已打开的调试模式浏览器
            print(f"🔗 正在连接到调试模式浏览器: {debugger_address}")
            edge_options = Options()
            edge_options.add_experimental_option("debuggerAddress", debugger_address)
            self.driver = webdriver.Edge(options=edge_options)  # ✅ 使用Options
            print("✅ 已连接到现有浏览器窗口")
        else:
            # 模式2：打开新浏览器
            print("🌐 正在打开新浏览器窗口")
            self.driver = webdriver.Edge()
            print("✅ 新浏览器已打开")

        # 等待浏览器元素加载完毕
        self.wait = WebDriverWait(self.driver, 10)

    def safe_refresh(self):
        """安全地刷新页面"""
        try:
            self.driver.refresh()
            print("  🔄 页面已刷新")
            return True
        except:
            print("  ⚠️ 刷新失败，重新打开页面")
            self.driver.get(self.url)
            time.sleep(2)
            return False

    def human_delay(self, min_sec=0.3, max_sec=0.8):
        """模拟人工延迟"""
        if self.human_like:
            delay = random.uniform(min_sec, max_sec)
            time.sleep(delay)

    def type_like_human(self, element, text):
        """模拟人工打字效果"""
        if not self.human_like:
            element.send_keys(text)
            return

        # 逐字输入，带随机延迟
        for char in text:
            element.send_keys(char)
            # 随机延迟 0.05-0.2 秒，模拟打字速度
            time.sleep(random.uniform(0.05, 0.2))

        # 输入完成后稍微停顿
        time.sleep(random.uniform(0.2, 0.5))

    def select_dropdown(self, by, value, gender_value):
        gender_select = self.wait.until(
            EC.presence_of_element_located((by, value))
        )
        select = Select(gender_select)
        select.select_by_value(gender_value)  # "男" 或 "女"

    def fill_input(self, by, value, content):
        """填充输入框 - 带人工效果"""
        try:
            # 1. 等待元素出现
            element = self.wait.until(EC.presence_of_element_located((by, value)))

            # 2. 滚动到可见区域
            self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
            self.human_delay(0.2, 0.5)

            # 3. 模拟鼠标移动到输入框
            if self.human_like:
                action = ActionChains(self.driver)
                action.move_to_element(element).perform()
                self.human_delay(0.1, 0.3)

            # 4. 点击输入框获得焦点
            element.click()
            self.human_delay(0.1, 0.2)

            # 5. 清空原有内容（模拟选中删除）
            element.clear()
            self.human_delay(0.1, 0.2)

            # 6. 模拟人工输入
            self.type_like_human(element, content)

            print(f"  ✓ 填写: {content}")

            # 7. 输入完成后按Tab键，模拟移出焦点
            if self.human_like:
                element.send_keys(Keys.TAB)
                self.human_delay(0.2, 0.4)

        except Exception as e:
            print(f"  ✗ 填充失败: {e}")

    def upload_file(self, by, value, file_path):
        """上传文件 - 带人工效果"""
        try:
            # 1. 找到上传区域并点击（模拟人工点击上传）
            upload_area = self.driver.find_element(By.CLASS_NAME, "photo-upload")

            # 滚动到可见
            self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", upload_area)
            self.human_delay(0.3, 0.6)

            # 模拟鼠标移动到上传区域
            if self.human_like:
                action = ActionChains(self.driver)
                action.move_to_element(upload_area).perform()
                self.human_delay(0.2, 0.4)

            # 点击上传区域（触发文件选择）
            # upload_area.click()
            # print("  📁 点击上传区域...")
            # self.human_delay(0.5, 1.0)

            # 2. 找到隐藏的file input并上传
            element = self.driver.find_element(by, value)
            element.send_keys(file_path)
            print(f"  📤 上传文件: {file_path}")

            # 3. 等待上传完成（模拟上传过程）
            self.human_delay(1.0, 1.5)

            # 4. 等待预览图出现
            try:
                self.wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, ".photo-preview img")))
                print("  🖼️  照片预览已加载")
                self.human_delay(0.5, 0.8)
            except:
                pass

        except Exception as e:
            print(f"  ✗ 上传失败: {e}")

    def click_button(self, by, value):
        """点击按钮 - 带人工效果"""
        try:
            # 1. 等待按钮可点击
            element = self.wait.until(EC.element_to_be_clickable((by, value)))

            # 2. 滚动到可见
            self.driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", element)
            self.human_delay(0.2, 0.5)

            # 3. 模拟鼠标移动到按钮
            if self.human_like:
                action = ActionChains(self.driver)
                action.move_to_element(element).perform()
                self.human_delay(0.3, 0.6)

            # 4. 模拟人工点击（先按下，再释放）
            if self.human_like:
                action = ActionChains(self.driver)
                action.click_and_hold(element).pause(0.1).release().perform()
            else:
                element.click()

            print(f"  ✓ 点击按钮: 保存")

            # 5. 点击后等待（模拟人工反应）
            self.human_delay(0.5, 1.0)

            return True

        except Exception as e:
            print(f"  ✗ 点击失败: {e}")
            return False

    def wait_for_save_complete(self):
        """等待保存完成"""
        try:
            # 等待成功消息出现
            success_msg = self.wait.until(
                EC.presence_of_element_located((By.CLASS_NAME, "message.success"))
            )
            print("  ✅ 保存成功！")

            # 等待成功消息消失（可选）
            if self.human_like:
                time.sleep(random.uniform(1.0, 2.0))  # 模拟人工查看结果

        except:
            print("  ⚠️  未检测到成功消息")

        # 等待2-3秒，模拟人工查看结果
        self.human_delay(1.5, 2.5)

    def submit_person(self, person_info):
        """提交单个人 - 带人工操作感"""
        print("\n  👤 开始录入人员信息...")

        # 1. 填写姓名（带打字效果）
        print("  ✏️  正在填写姓名...")
        self.fill_input(By.ID, "name", person_info['name'])

        print("  ✏️  正在选择性别...")
        self.select_dropdown(By.ID, "gender", person_info['gender'])

        # 2. 上传照片（带点击和等待）
        if person_info.get('photo'):
            print("  📸 正在上传照片...")
            self.upload_file(By.ID, "photoInput", person_info['photo'])

        # 3. 等待一下再点击保存（模拟人工确认）
        self.human_delay(0.5, 1.0)

        # 4. 点击保存
        print("  💾 正在保存...")
        self.click_button(By.CLASS_NAME, "btn-save")

        # 5. 等待保存完成
        self.wait_for_save_complete()

    def batch_submit_from_excel(self, excel_path):
        """从Excel批量读取并提交"""
        df = pd.read_excel(excel_path)
        print(f"📊 读取到 {len(df)} 条数据\n")

        # 打开页面
        self.driver.get(self.url)
        print("🌐 页面已打开")

        # 等待页面完全加载
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

            # 提交人员信息
            self.submit_person(person_info)

            # 如果是最后一条，不刷新
            if index < len(df) - 1:
                print("\n  🔄 准备录入下一条...")
                # 模拟人工点击刷新或重新加载
                self.human_delay(1.0, 1.5)
                self.driver.refresh()
                print("  🔄 页面已刷新")
                self.human_delay(1.0, 1.5)

        print(f"\n{'=' * 60}")
        print(f"✅ 全部完成！共处理 {len(df)} 人")
        print(f"{'=' * 60}")

    def close(self):
        if self.keep_browser_open:
            print("\n🌐 浏览器保持打开状态，请手动关闭")
            # 不执行 self.driver.quit()
        else:
            self.driver.quit()


if __name__ == "__main__":
    url = "F:/Projects/Study/github/Pycharm/personWithGender.html"
    excel_file = "F:/Projects/Study/github/Pycharm/person.xlsx"
    # 创建自动化实例
    auto = SeleniumAutoSubmit(url, keep_browser_open=True, human_like=True, use_debug_mode=False)

    try:
        auto.batch_submit_from_excel(excel_file)
        input("\n按Enter键结束程序...")
    finally:
        auto.close()
