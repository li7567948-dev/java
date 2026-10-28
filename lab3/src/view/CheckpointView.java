package view;

import model.Checkpoint;
import model.Vehicle;
import model.SolvabilityService;

/** View is responsible only for displaying data (MVC). */
public final class CheckpointView {
    public void header() {
        System.out.println("Лабораторная работа №3: исключения и коллекции");
        System.out.println("Тема ЛР №2: Пункт пропуска автомобилей");
        System.out.println("--------------------------------------------");
    }

    public void collections(int arrayCount, int mapCount) {
        System.out.println("ArrayList<Checkpoint>: " + arrayCount + " объектов");
        System.out.println("HashMap<Integer, Checkpoint>: " + mapCount + " объектов");
    }

    public void checkpoint(Checkpoint checkpoint) {
        System.out.println("\n" + checkpoint.getName());
        System.out.println("Лимит: " + checkpoint.getLimitMinutes() + " мин");
        for (Vehicle vehicle : checkpoint.getVehicles()) {
            System.out.println("  " + vehicle + "; водитель: " + vehicle.getDriver().getName());
        }
    }

    public void solvability(SolvabilityService.Result result) {
        System.out.println("Режим разрешимости: " +
                (result.isFeasible() ? "план выполним" : "нужны изменения"));
        System.out.println(result.getExplanation());
    }
}
