package com.dji.GSDemo.GoogleMap;

public class ExtendedPathPoint extends PathPoint{
    private String instruction;
    private String task;

    public ExtendedPathPoint(double YAltitude, double ZLatitude, double XLongitude, int ID, String instruction, String task){
        super(YAltitude, ZLatitude, XLongitude, ID);
        this.instruction = instruction;
        this.task = task;
    }

    public ExtendedPathPoint(){
        super();
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }
}

