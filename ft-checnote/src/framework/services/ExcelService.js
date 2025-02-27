import * as xlsx from 'xlsx';

// https://blog.csdn.net/aahwj/article/details/135232367
// https://blog.csdn.net/mochenangel/article/details/114650985
export class ExcelService {
    /*static ExportToExcel(excelName = 'data.xlsx') {
        // xlsx库要求将数据转换为适当的格式才能导出为Excel文件。最常用的格式是二维数组，其中每个子数组代表一行数据。确保准备好的数据结构与xlsx库的要求相匹配。
        let data = [['Name', 'Age', 'Email'], ['John Doe', 30, 'johndoe@example.com'], ['Jane Smith', 25, 'janesmith@example.com']];

        // 创建一个工作簿
        const workBook = xlsx.utils.book_new();
        // 创建一个工作表
        const workSheet = xlsx.utils.aoa_to_sheet(data);
        // 将工作表添加到工作簿
        xlsx.utils.book_append_sheet(workBook, workSheet, 'Sheet1');
        xlsx.writeFile(workBook, excelName);
    }*/

    static _AutoWidth(ws, data) {
        /*set worksheet max width per col*/
        const colWidth = data.map(row => row.map(val => {
            /*if null/undefined*/
            if (val == null) {
                return {'wch': 10};
            }
            /*if chinese*/
            else if (val.toString().charCodeAt(0) > 255) {
                return {'wch': val.toString().length * 2};
            } else {
                return {'wch': val.toString().length};
            }
        }))
        /*start in the first row*/
        let result = colWidth[0];
        for (let i = 1; i < colWidth.length; i++) {
            for (let j = 0; j < colWidth[i].length; j++) {
                if (result[j]['wch'] < colWidth[i][j]['wch']) {
                    result[j]['wch'] = colWidth[i][j]['wch'];
                }
            }
        }
        ws['!cols'] = result;
    }

    // get head from excel file,return array
    static GetHeaderRow(sheet) {
        const headers = []
        const range = xlsx.utils.decode_range(sheet['!ref'])
        const R = range.s.r /* start in the first row */
        for (let C = range.s.c; C <= range.e.c; ++C) { /* walk every column in the range */
            let cell = sheet[xlsx.utils.encode_cell({c: C, r: R})] /* find the cell in the first row */
            let hdr = 'UNKNOWN ' + C // <-- replace with your desired default
            if (cell && cell.t) hdr = xlsx.utils.format_cell(cell)
            headers.push(hdr)
        }
        return headers
    }

    // 对象数组 转换为 二维数组
    static OOA_TO_AOA(ooa) {
        return ooa.map((o) => {
            return Object.values(o);
        });
    }

    static ExportAOAToExcel1(aoaWithoutTitle, arrayTitle, filename, autoWidth = true) {
        const aoaWithTitle = aoaWithoutTitle;
        aoaWithTitle.unshift(arrayTitle);
        ExcelService.ExportAOAToExcel2(aoaWithTitle, filename, autoWidth);
    }

    // aoa 即 array of array二维数组
    static ExportAOAToExcel2(aoaWithTitle, filename, autoWidth = true) {
        // 创建一个工作簿
        const wb = xlsx.utils.book_new();
        // 创建一个工作表
        const ws = xlsx.utils.aoa_to_sheet(aoaWithTitle);
        if (autoWidth) {
            ExcelService._AutoWidth(ws, aoaWithTitle);
        }
        xlsx.utils.book_append_sheet(wb, ws, filename);
        xlsx.writeFile(wb, filename + '.xlsx');
    }

    // ooa即：object of array对象数组
}
