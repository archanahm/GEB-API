package com.hbg.util

/**
 * Created by Joseph Sebastian on 10/04/2018.
 */
class CommonUtils {

    private static List<String> keysToOverride = ["REQUEST.Client_id"]

    static Map overrideValues(Map valuesMap) {
        keysToOverride.each {
            def value = System.getProperty(it)
            if (value) valuesMap[it] = value
        }
        valuesMap
    }

    def static flat(List lst, int size) {
        List result = []
        lst.each { element ->
            List row = []
            for (int i = 0; i < size; i++) {
                def item = element[i]
                if (item instanceof List) {
                    (List) item.each { value ->
                        row << value
                    }
                } else
                    row << item
            }
            result << row
        }
        result
    } 

} 
