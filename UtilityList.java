package CLHUIMiner;

import java.util.*;

public class UtilityList {
    public Integer item;
    public long sumIutils = 0;
    public long sumRutils = 0;
    public List<Element> elements = new ArrayList<Element>();

    public UtilityList(Integer item){
        this.item = item;
    }

    public void addElement(Element element){
        sumIutils += element.iutils;
        sumRutils += element.rutils;
        elements.add(element);
    }

    public int getSupport(){
        return elements.size();
    }

    public long getUtils(){
        return this.sumIutils;
    }

}
