export class MathUtil {
    static Swap(arr, leftIndex, rightIndex) {
        let t = arr[leftIndex];
        arr[leftIndex] = arr[rightIndex];
        arr[rightIndex] = t;
    }
}