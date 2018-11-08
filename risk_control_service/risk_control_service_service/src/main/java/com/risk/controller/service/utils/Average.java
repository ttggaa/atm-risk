package com.risk.controller.service.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Average {

    private static DecimalFormat df = new DecimalFormat("#.00");

    public static String getAverage(int[] array){
        double sum = 0;
        double num = 0;
        int biggest = array[0];
        int smallest = array[0];
        Double result=0D;
        for(int i=0;i<array.length-1;i++){
            if(array[i]>biggest){
                biggest = array[i];
            }
            if(array[i]<smallest){
                smallest = array[i];
            }
        }
        for(int j=0;j<array.length;j++){
            if(array[j] == biggest || array[j] == smallest){
            }else{
                sum += (double)array[j];
                num++;
            }
        }
        result = sum/num;

        return df.format(result);
    }

    public static String getAverages(List<Integer> list){
        if (null==list||list.isEmpty()){
            return "";
        }
        Integer size =list.size();
        Double result = 0D;
        Double re=0D;
//        switch (size){
//            case 6:
//                re = list.get(1).doubleValue()+list.get(2).doubleValue()+list.get(3).doubleValue()
//                +list.get(4).doubleValue();
//                result = re/4;
//                break;
//            default:
//                return "";
//        }
        if (size >= 3) {
            for (int i = 0; i < size; i++) {
                if (i != 0 && i != (size - 1)) {
                    re += list.get(i).doubleValue();
                }
            }
            result = re/(size - 2);

        } else {
            return "";
        }

        result = result/100;
        if (result<1){
            return result.toString();
        }
        return df.format(result);
    }

    public static void main(String[] args) {
//        int[] array = {5,4,10,8,1};
//        System.out.println(getAverage(array));

//        Double a= 1/100D;
//        System.out.println(df.format(a));


        List<Integer> nums = new ArrayList<Integer>();
        nums.add(3);
        nums.add(5);
        nums.add(1);
        nums.add(4);
        nums.add(0);
        nums.add(3);

        Collections.sort(nums);
        System.out.println(nums);
        System.out.println(getAverages(nums));
    }
}
