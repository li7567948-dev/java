package model;
public class Trailer extends Vehicle {
    public Trailer(String number,double length,double width,double height,double weight,int speed,String cargo) {
        super(number,length,width,height,weight,speed,cargo);
    }
    @Override public String getType(){return "Трейлер";}
    @Override public double getMaxWeight(){return 44000;}
    @Override public double getMaxLength(){return 20.0;}
    @Override public double getMaxWidth(){return 2.55;}
    @Override public double getMaxHeight(){return 4.0;}
    @Override public int getMaxSpeed(){return 30;}
    @Override public boolean pass() {
        boolean ok=super.pass();
        if(getCargo().equals(BOOM)&&getSpeed()>10){
            reason+="Скорость с взрывоопасным грузом больше 10 км/ч; ";
            ok=false;
        }
        return ok;
    }
}