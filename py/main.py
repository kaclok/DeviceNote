from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time
import pandas as pd


class SeleniumAutoSubmit:
    def __init__(self, url, keep_browser_open=True):
        self.url = url
        self.driver = webdriver.Edge()
        self.wait = WebDriverWait(self.driver, 10)
        self.keep_browser_open = keep_browser_open

    def fill_input(self, by, value, content):
        """填充输入框"""
        try:
            element = self.wait.until(EC.presence_of_element_located((by, value)))
            element.clear()
            element.send_keys(content)
            print(f"  填充: {value} = {content}")
        except Exception as e:
            print(f"  填充失败: {e}")

    def upload_file(self, by, value, file_path):
        """上传文件"""
        try:
            element = self.driver.find_element(by, value)
            element.send_keys(file_path)
            print(f"  上传文件: {file_path}")
        except Exception as e:
            print(f"  上传失败: {e}")

    def click_button(self, by, value):
        """点击按钮"""
        try:
            element = self.wait.until(EC.element_to_be_clickable((by, value)))
            element.click()
            print(f"  点击: {value}")
            time.sleep(3)
            return True
        except Exception as e:
            print(f"  点击失败: {e}")
            return False

    def submit_person(self, person_info):
        """提交单个人"""
        # 根据实际网页调整
        self.fill_input(By.ID, "name", person_info['name'])

        # 上传证件照
        if person_info.get('photo'):
            self.upload_file(By.ID, "photoInput", person_info['photo'])

        # 提交
        self.click_button(By.CLASS_NAME, "btn-save")

    def batch_submit_from_excel(self, excel_path):
        """从Excel批量读取并提交"""
        df = pd.read_excel(excel_path)

        self.driver.get(self.url)

        for index, row in df.iterrows():
            print(f"\n正在录入第 {index + 1} 人: {row['name']}")

            person_info = {
                'name': row['name'],
                'photo': row['photo']
            }

            self.submit_person(person_info)
            time.sleep(2)

            # 重新加载页面（如果需要）
            self.driver.get(self.url)

        print(f"\n全部完成！共 {len(df)} 人")

    def close(self):
        if self.keep_browser_open:
            print("浏览器保持打开状态，请手动关闭")
            # 不执行 self.driver.quit()
        else:
            self.driver.quit()


if __name__ == "__main__":
    # 注意：DeepSeek网站不是表单提交页面，需要替换为实际的表单页面URL
    # url = input("请输入目标网页URL: ")
    # excel_file = input("请输入Excel文件路径: ")

    url = "F:/Projects/Study/github/Pycharm/person.html"
    excel_file = "F:/Projects/Study/github/Pycharm/person.xlsx"

    auto = SeleniumAutoSubmit(url)
    try:
        auto.batch_submit_from_excel(excel_file)
    finally:
        auto.close()
