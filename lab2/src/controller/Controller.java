package controller;

import model.Event;
import model.CheckResult;
import model.Simulation;
import model.Vehicle;
import model.VehicleFactory;
import view.ConsoleView;

public class Controller {
    private final ConsoleView view;
    public Controller(ConsoleView view){this.view=view;}
    public void start(){
        int choice;
        do{
            view.showMenu();
            choice=view.readInt("Выбор: ");
            handleChoice(choice);
        }while(choice!=0);
    }
    private void handleChoice(int choice){
        switch(choice){
            case 1:checkReadyVehicles();break;
            case 2:checkOwnVehicle();break;
            case 3:runSimulation();break;
            case 0:view.showMessage("Выход из программы");break;
            default:view.showMessage("Неверный выбор");
        }
    }
    private void showCheck(Vehicle vehicle){view.showCheckResult(new CheckResult(vehicle));}
    private void checkReadyVehicles(){
        view.showMessage("===== Проверка готовых машин =====");
        for(Vehicle vehicle:VehicleFactory.readyVehicles()){
            showCheck(vehicle);
            view.showMessage("--------------------");
        }
    }
    private void checkOwnVehicle(){
        view.showMessage("===== Проверка своей машины =====");
        view.showMessage("1 - Легковой автомобиль");
        view.showMessage("2 - Грузовой автомобиль");
        view.showMessage("3 - Автобус");
        view.showMessage("4 - Трейлер");
        view.showMessage("5 - Автомобиль с прицепом");
        int type=view.readInt("Тип машины: ");
        if(type<1||type>5){view.showMessage("Неверный тип машины");return;}
        String number=view.readString("Номер: ");
        double length=view.readDouble("Длина (м): ");
        double width=view.readDouble("Ширина (м): ");
        double height=view.readDouble("Высота (м): ");
        double weight=view.readDouble("Масса (кг): ");
        int speed=view.readInt("Скорость (км/ч): ");
        for(int i=0;i<Vehicle.CARGO.length;i++)
            view.showMessage((i+1)+" - "+Vehicle.CARGO[i]);
        int cargoChoice=view.readInt("Тип груза: ");
        if(cargoChoice<1||cargoChoice>Vehicle.CARGO.length){
            view.showMessage("Неверный тип груза");return;
        }
        try{
            Vehicle vehicle=VehicleFactory.create(type,number,length,width,height,weight,speed,Vehicle.CARGO[cargoChoice-1]);
            showCheck(vehicle);
        }catch(IllegalArgumentException e){view.showMessage("Ошибка: "+e.getMessage());}
    }
    private void runSimulation(){
        int pointCount=view.readInt("Количество пунктов: ");
        int duration=view.readInt("Время моделирования: ");
        if(pointCount<=0||pointCount>100||duration<=0||duration>100000){
            view.showMessage("Неверные параметры");return;
        }
        Simulation simulation=new Simulation(pointCount);
        view.showMessage("1 - Три готовые машины");
        view.showMessage("2 - Случайный поток машин");
        int mode=view.readInt("Режим: ");
        if(mode==1)simulation.runReady(duration);
        else if(mode==2){
            int min=view.readInt("Минимальный интервал прибытия (мин): ");
            int max=view.readInt("Максимальный интервал прибытия (мин): ");
            if(min<=0||max<min){view.showMessage("Неверный интервал");return;}
            simulation.runGenerated(duration,min,max);
        }else{view.showMessage("Неверный режим");return;}
        view.showMessage("===== РЕЗУЛЬТАТЫ =====");
        view.showMessage("Время: "+simulation.getTime());
        view.showMessage("Проверено: "+simulation.getCompleted());
        view.showMessage("Разрешено: "+simulation.getAllowed());
        view.showMessage("Запрещено: "+simulation.getDenied());
        view.showMessage("В очереди: "+simulation.getQueueSize());
        view.showMessage("На проверке: "+simulation.getActiveCount());
        view.showMessage("===== СОБЫТИЯ =====");
        for(Event event:simulation.getEvents())view.showMessage(event.toString());
    }
}