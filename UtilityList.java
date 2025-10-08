package CLHMiner;

import java.util.*;

public class UtilityList {
	Integer item;
	int sumIutils = 0;
	int sumRutils = 0;
	 List<Element> elements = new ArrayList<Element>();
	 List<UtilityList> childs = new ArrayList<UtilityList>();
	 int GWU = 0;

	public UtilityList(Integer item){
		this.item = item;
	}

	public void addElement(Element element){
		sumIutils += element.iutils;
		sumRutils += element.rutils;
		elements.add(element);
		GWU+=element.TU;
	}
	
	public int getSupport() {
		return elements.size();
	}
	
	public List<Element> getElement(){
		return elements;
	}
	public List<UtilityList> getChild(){
		return childs;
	}
	public void AddChild(UtilityList uLs) {	
		this.childs.add(uLs);
	}
}
