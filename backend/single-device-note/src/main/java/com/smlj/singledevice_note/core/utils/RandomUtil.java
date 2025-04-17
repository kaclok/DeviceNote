package com.smlj.singledevice_note.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomUtil {
    public static <T> List<T> RandomList(ArrayList<T> array, int count) {
        if(array == null || array.isEmpty()) {
            return null;
        }

        count = Math.min(count, array.size());
        List<T> list = new ArrayList<>(array);
        List<T> selected = new ArrayList<>();
        Random random = new Random();
        while (selected.size() < count && !list.isEmpty()) {
            int index = random.nextInt(list.size());
            selected.add(list.remove(index));
        }
        return selected;
    }
}
