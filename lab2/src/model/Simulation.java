package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Simulation {
    private final List<CheckPoint> points=new ArrayList<>();
    private final Queue<Vehicle> queue=new LinkedList<>();
    private final List<Event> events=new ArrayList<>();
    private int time,completed,allowed,denied;

    public Simulation(int pointCount){
        if(pointCount<=0)throw new IllegalArgumentException("Количество пунктов должно быть положительным");
        for(int i=0;i<pointCount;i++){
            CheckPoint point=new CheckPoint(i+1);
            point.setOpen(true);
            points.add(point);
        }
    }
    public void setPointOpen(int id,boolean open){
        if(id<1||id>points.size())throw new IllegalArgumentException("Неверный номер пункта");
        points.get(id-1).setOpen(open);
        events.add(new Event(time,open?"Пункт открыт":"Пункт закрыт",null,id));
    }
    public void addVehicle(Vehicle vehicle){
        if(vehicle==null)throw new IllegalArgumentException("Машина отсутствует");
        vehicle.setArriveTime(time);
        queue.add(vehicle);
        events.add(new Event(time,"Автомобиль прибыл",vehicle,0));
    }
    public void step(){
        for(CheckPoint point:points){
            if(point.isOpen()&&point.isFree()&&!queue.isEmpty()){
                Vehicle vehicle=queue.poll();
                point.take(vehicle);
                events.add(new Event(time,"Начало проверки",vehicle,point.getId()));
            }
        }
        time++;
        for(CheckPoint point:points){
            Vehicle done=point.work();
            if(done!=null){
                completed++;
                if(done.pass()){
                    allowed++;
                    events.add(new Event(time,"Проверка завершена: разрешено",done,point.getId()));
                }else{
                    denied++;
                    events.add(new Event(time,"Проверка завершена: запрещено ("+done.getReason()+")",done,point.getId()));
                }
            }
        }
    }
    public void run(int duration){
        if(duration<0)throw new IllegalArgumentException("Отрицательное время");
        for(int i=0;i<duration;i++)step();
    }
    public void runReady(int duration){
        Vehicle[] vehicles=VehicleFactory.readyVehicles();
        for(int i=0;i<3;i++)addVehicle(vehicles[i]);
        run(duration);
    }
    public void runGenerated(int duration,int minInterval,int maxInterval){
        if(duration<0)throw new IllegalArgumentException("Отрицательное время");
        Generator generator=new Generator(minInterval,maxInterval);
        for(int i=0;i<duration;i++){
            for(Vehicle vehicle:generator.newCars(time))addVehicle(vehicle);
            step();
        }
    }
    public int getTime(){return time;}
    public int getCompleted(){return completed;}
    public int getAllowed(){return allowed;}
    public int getDenied(){return denied;}
    public int getQueueSize(){return queue.size();}
    public int getActiveCount(){
        int count=0;
        for(CheckPoint point:points)if(!point.isFree())count++;
        return count;
    }
    public List<Event> getEvents(){return Collections.unmodifiableList(events);}
}