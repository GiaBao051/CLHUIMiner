package CLHMiner;

public class Element {
	public int tid ;   
	public double iutils;   
	public double rutils; 
	public double TU;

	public Element(int tid, double iutils, double rutils,double TU){
		this.tid = tid;
		this.iutils = iutils;
		this.rutils = rutils;
		this.TU=TU;
	}

	public Element(int tid){
		this.tid = tid;
		this.iutils = 0;
		this.rutils = 0;
	}
	
	public Element(int tid,double iutils){
		this.tid = tid;
		this.iutils = iutils;
		this.rutils = 0;
	}
}
