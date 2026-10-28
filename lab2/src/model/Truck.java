package model;
public class Truck extends Vehicle {
    public Truck(String number,double length,double width,double height,double weight,int speed,String cargo) {
        super(number,length,width,height,weight,speed,cargo);
    }
    @Override public String getType(){return "Грузовой автомобиль";}
    @Override public double getMaxWeight(){return 40000;}
    @Override public double getMaxLength(){return 12.0;}
    @Override public double getMaxWidth(){return 2.5;}
    @Override public double getMaxHeight(){return 4.0;}
    @Override public int getMaxSpeed(){return 70;}
}