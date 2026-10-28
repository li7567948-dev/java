package view;

import java.util.Scanner;
import model.CheckResult;

public class ConsoleView {
    private Scanner scanner=new Scanner(System.in);
    public void showMenu(){
        System.out.println();
        System.out.println("===== ПУНКТ ПРОПУСКА =====");
        System.out.println("1 - Проверка готовых машин");
        System.out.println("2 - Проверить свою машину");
        System.out.println("3 - Моделирование очереди");
        System.out.println("0 - Выход");
    }
    public void showMessage(String text){System.out.println(text);}
    public void showCheckResult(CheckResult result){
        System.out.println(result.getVehicle());
        System.out.println("Результат: "+(result.isAllowed()?"разрешено":"запрещено"));
        if(!result.isAllowed())System.out.println("Причина: "+result.getReason());
        System.out.println("Время проверки: "+result.getCheckTime()+" мин");
    }
    public String readString(String text){
        System.out.print(text);
        return scanner.next();
    }
    public int readInt(String text){
        while(true){
            System.out.print(text);
            String value=scanner.next();
            try{return Integer.parseInt(value);}
            catch(NumberFormatException e){System.out.println("Ошибка! Введите целое число.");}
        }
    }
    public double readDouble(String text){
        while(true){
            System.out.print(text);
            String value=scanner.next().replace(',','.');
            try{return Double.parseDouble(value);}
            catch(NumberFormatException e){System.out.println("Ошибка! Введите число.");}
        }
    }
}