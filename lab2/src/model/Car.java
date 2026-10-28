package model;
public class Car extends Vehicle {
    public Car(String number,double length,double width,double height,double weight,int speed,String cargo) {
        super(number,length,width,height,weight,speed,cargo);
    }
    @Override public String getType(){return "Легковой автомобиль";}
    @Override public double getMaxWeight(){return 3500;}
    @Override public double getMaxLength(){return 6.0;}
    @Override public double getMaxWidth(){return 2.5;}
    @Override public double getMaxHeight(){return 3.0;}
    @Override public int getMaxSpeed(){return 90;}
}