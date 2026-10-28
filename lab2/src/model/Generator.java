package model;

import java.util.ArrayList;
import java.util.Random;

public class Generator {
    private Random rnd=new Random();
    private int min,max,next=0,numberCounter=0;
    public Generator(int min,int max){
        if(min<1||max<min||max==Integer.MAX_VALUE)
            throw new IllegalArgumentException("Неверный интервал прибытия");
        this.min=min;
        this.max=max;
    }
    public ArrayList<Vehicle> newCars(int minute){
        ArrayList<Vehicle> list=new ArrayList<>();
        while(next<=minute){
            list.add(randomCar());
            next+=randomInt(min,max);
        }
        return list;
    }
    private int randomInt(int a,int b){return a+rnd.nextInt(b-a+1);}
    private double randomDouble(double a,double b){
        return Math.round((a+rnd.nextDouble()*(b-a))*10)/10.0;
    }
    public Vehicle randomCar(){
        String number="A"+(++numberCounter)+"BC";
        String cargo=Vehicle.SAFE;
        if(rnd.nextInt(4)==0)cargo=Vehicle.CARGO[randomInt(0,4)];
        int type=randomInt(0,4);
        if(type==0)return new Car(number,randomDouble(3.5,5.1),randomDouble(1.6,2.0),randomDouble(1.4,2.0),randomDouble(1000,3600),randomInt(20,62),cargo);
        if(type==1)return new Truck(number,randomDouble(6.0,12.2),randomDouble(2.2,2.55),randomDouble(2.8,4.1),randomDouble(5000,41000),randomInt(10,72),cargo);
        if(type==2)return new Bus(number,randomDouble(8.0,15.0),randomDouble(2.3,2.55),randomDouble(2.8,4.0),randomDouble(8000,18500),randomInt(15,62),cargo);
        if(type==3)return new Trailer(number,randomDouble(14.0,20.3),randomDouble(2.45,2.55),randomDouble(3.5,4.05),randomDouble(15000,45000),randomInt(5,32),cargo);
        return new CarWithTrailer(number,randomDouble(6.0,10.2),randomDouble(1.8,2.2),randomDouble(1.5,3.0),randomDouble(2000,5200),randomInt(15,42),cargo);
    }
}