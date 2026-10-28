package model;
import java.util.Objects;

public abstract class Vehicle implements CargoCheck {
    public static final String SAFE="безопасный";
    public static final String FIRE="огнеопасный";
    public static final String CHEMICAL="химически опасный";
    public static final String BOOM="взрывоопасный";
    public static final String SPECIAL="особой важности";
    public static final String[] CARGO={SAFE,FIRE,CHEMICAL,BOOM,SPECIAL};
    private String number;
    private double length,width,height,weight;
    private int speed;
    private String cargo;
    private int arriveTime;
    protected String reason="";

    protected Vehicle(String number,double length,double width,double height,double weight,int speed,String cargo) {
        if(number==null||number.trim().isEmpty()
            ||!Double.isFinite(length)||length<=0
            ||!Double.isFinite(width)||width<=0
            ||!Double.isFinite(height)||height<=0
            ||!Double.isFinite(weight)||weight<=0||speed<0)
            throw new IllegalArgumentException("Неверные параметры машины");
        if(!isValidCargo(cargo)) throw new IllegalArgumentException("Неизвестный тип груза");
        this.number=number;this.length=length;this.width=width;this.height=height;
        this.weight=weight;this.speed=speed;this.cargo=cargo;
    }
    // Доступ по умолчанию (package-private): метод доступен внутри пакета model.
    static boolean isValidCargo(String cargo) {
        for(String item:CARGO) {
            if(item.equals(cargo)) return true;
        }
        return false;
    }

    public String getNumber(){return number;}
    public double getLength(){return length;}
    public double getWidth(){return width;}
    public double getHeight(){return height;}
    public double getWeight(){return weight;}
    public int getSpeed(){return speed;}
    public String getCargo(){return cargo;}
    public String getReason(){return reason;}
    public int getArriveTime(){return arriveTime;}
    public void setArriveTime(int arriveTime){this.arriveTime=arriveTime;}
    public abstract String getType();
    public abstract double getMaxLength();
    public abstract double getMaxWidth();
    public abstract double getMaxHeight();
    public abstract double getMaxWeight();
    public abstract int getMaxSpeed();
    @Override public boolean isDangerous(){return FIRE.equals(cargo)||CHEMICAL.equals(cargo)||BOOM.equals(cargo);}
    public int getCheckTime(){
        int time;
        if(length<=5)time=1;
        else if(length<=10)time=2;
        else if(length<=15)time=3;
        else time=4;
        if(FIRE.equals(cargo)||SPECIAL.equals(cargo))time+=2;
        if(CHEMICAL.equals(cargo))time+=3;
        if(BOOM.equals(cargo))time+=5;
        return time;
    }
    public boolean pass(){
        reason="";
        if(length>getMaxLength())reason+="длина больше нормы; ";
        if(width>getMaxWidth())reason+="ширина больше нормы; ";
        if(height>getMaxHeight())reason+="высота больше нормы; ";
        if(weight>getMaxWeight())reason+="масса больше нормы; ";
        int maxSpeed=getMaxSpeed();
        if(isDangerous()&&maxSpeed>20)maxSpeed=20;
        if(speed>maxSpeed)reason+="скорость больше нормы; ";
        return reason.isEmpty();
    }
    @Override public String toString(){
        return getType()+" "+number+": "+length+"x"+width+"x"+height+" м, "+weight+" кг, "+speed+" км/ч, груз "+cargo;
    }
    @Override public boolean equals(Object obj){
        if(this==obj)return true;
        if(!(obj instanceof Vehicle))return false;
        return Objects.equals(number,((Vehicle)obj).number);
    }
    @Override public int hashCode(){return Objects.hash(number);}
}