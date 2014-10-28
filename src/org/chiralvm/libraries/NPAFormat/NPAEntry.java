package org.chiralvm.libraries.NPAFormat;

import java.io.File;

public class NPAEntry {
	private String name;
	private int id,offset,csize,size,type,position; 
	
	public NPAEntry() {}
	
	public NPAEntry(
	String name,
	int position,
	int type,
	int id,
	int offset,
	int csize,
	int size) {
		this.name=name;
		this.position=position;
		this.type=type;
		this.offset=offset;
		this.csize=csize;
		this.size=size;
	}
	
	public String getName() { return name.replaceAll("\\\\", File.separator); }
	public String getFilename() { return (name.contains("\\\\") ? name.split("\\\\")[1] : name); }
	
	public String getOriginalName() { return name;}
	public void setOriginalName(String name) { this.name = name; }	

	public int getID() { return id; }
	public void setID(int id) { this.id=id; }
	
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position=position; }
	
	public int getOffset() { return offset; }
	public void setOffset(int offset) { this.offset=offset; }
	
	public int getType() {return type;}
	public void setType(int type) { this.type=type; }
	
	public boolean isFile() { return (type==2); }
	public boolean isDirectory() { return (type==1); }
	public boolean isUnknown() { return (type!=1&&type!=2); }
	
	public int getCSize() { return csize;}
	public void setCSize(int csize) { this.csize=csize; }
	
	public int getSize() { return size; }
	public void setSize(int size) { this.size=size; }
	
}
