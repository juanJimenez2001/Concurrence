
package cc.banco;


import es.upm.aedlib.Pair;
import es.upm.babel.cclib.Monitor;

import java.util.*;

public class copia implements Banco {

    private boolean permiso_transf = false;
    private boolean permiso_minimo = false;
    private Map<String, Integer> cuentas;
    private Map<String,Pair<Monitor.Cond,Monitor.Cond>> no_creadas;
    private Monitor mutex;
    private Map<String,Pair<Queue<String>,Queue<Integer>>> transferencias;
    private Map<String,Queue<Integer>> alertas;


    public copia() {
        cuentas = new HashMap<String, Integer>();
        no_creadas = new HashMap<String,Pair<Monitor.Cond,Monitor.Cond>>();
        mutex = new Monitor();
        transferencias = new HashMap<String,Pair<Queue<String>,Queue<Integer>>>();
        alertas = new HashMap<String,Queue<Integer>>();
    }

    /**
     * Un cajero pide que se ingrese una determinado valor v a una
     * cuenta  Entregas disponibles: ∞ c. Si la cuenta no existe, se crea.
     *
     * @param c número de cuenta
     * @param v valor a ingresar
     */
    public void ingresar(String c, int v) {
        mutex.enter();
        if (!cuentas.containsKey(c)) {
            cuentas.put(c,v);
            if(!no_creadas.containsKey(c)) no_creadas.put(c,new Pair<Monitor.Cond,Monitor.Cond>(mutex.newCond(),mutex.newCond()));
        } else {
            cuentas.put(c,cuentas.get(c)+v);
        }
        desbloqueo_transf();
        mutex.leave();
    }


    /**
     * Un orde Entregas disponibles: ∞ nante pide que se transfiera un determinado valor v desde
     * una cuenta o a otra cuenta d.
     *
     * @param o número de cuenta origen
     * @param d número de cuenta destino
     * @param v valor a transferir
     * @throws IllegalArgumentException si o y d son las mismas cuentas
     */
    public void transferir(String o, String d, int v) {
        if (o.equals(d)) throw new IllegalArgumentException();
        mutex.enter();
        if(!cuentas.containsKey(o) || !cuentas.containsKey(d) || cuentas.get(o)<v || transferencias.containsKey(o)) {
            if(transferencias.containsKey(o)){
                transferencias.get(o).getLeft().add(d);
                transferencias.get(o).getRight().add(v);
            }
            else{
                Queue<String> aux;
                Queue<Integer> aux2;
                aux = new LinkedList<>();
                aux2 = new LinkedList<>();
                aux.add(d);
                aux2.add(v);
                transferencias.put(o,new Pair<Queue<String>,Queue<Integer>>(aux,aux2));
            }
            if(!no_creadas.containsKey(o)) no_creadas.put(o,new Pair<Monitor.Cond,Monitor.Cond>(mutex.newCond(),mutex.newCond()));
            no_creadas.get(o).getLeft().await();
            while(!permiso_transf){}
            permiso_transf = false;
        }
        cuentas.put(o,cuentas.get(o)-v);
        desbloqueo_minimo();
        mutex.leave();
        mutex.enter();
        cuentas.put(d,cuentas.get(d)+v);
        desbloqueo_transf();
        mutex.leave();
    }

    private void desbloqueo_transf(){
        Map.Entry<String,Pair<Queue<String>,Queue<Integer>>> aux = null;
        for(Map.Entry<String,Pair<Queue<String>,Queue<Integer>>> it : transferencias.entrySet()){
            if(it.getValue().getLeft().isEmpty()) continue;
            if(cuentas.containsKey(it.getKey()) && cuentas.containsKey(it.getValue().getLeft().peek()) && cuentas.get(it.getKey())>=it.getValue().getRight().peek()){
                it.getValue().getLeft().poll();
                it.getValue().getRight().poll();
                aux = it;
                break;
            }
        }

        if(aux != null){
            if(aux.getValue().getLeft().isEmpty()) transferencias.remove(aux.getKey());
            no_creadas.get(aux.getKey()).getLeft().signal();
        }
        permiso_transf=true;
    }

    private void desbloqueo_minimo(){
        Map.Entry<String,Queue<Integer>> aux = null;
        for(Map.Entry<String,Queue<Integer>> it : alertas.entrySet()){
            if(it.getValue().isEmpty()) continue;
            if(cuentas.containsKey(it.getKey()) && cuentas.get(it.getKey()) < it.getValue().peek()){
                aux = it;
                it.getValue().poll();
                break;
            }
        }

        if(aux != null){
            if(aux.getValue().isEmpty()) alertas.remove(aux.getKey());
            no_creadas.get(aux.getKey()).getRight().signal();
        }
        permiso_minimo = true;
    }

    /**
     * Un consultor pide el saldo disponible de una cuenta c.
     *
     * @param c número de la cuenta
     * @return saldo disponible en la cuenta id
     * @throws IllegalArgumentException si la cuenta c no existe
     */
    public int disponible(String c) {
    	mutex.enter();
        if (!cuentas.containsKey(c)) throw new IllegalArgumentException();
        int aux = cuentas.get(c);
        mutex.leave();
        return aux;
    }

    /**
     * Un avisador establece una alerta para la cuenta c. La operación
     * termina cuando el saldo de la cuenta c baja por debajo de m.
     *
     * @param c número de la cuenta
     * @param m saldo mínimo
     * @throws IllegalArgumentException si la cuenta c no existe
     */
    public void alertar(String c, int m) {
    	mutex.enter();
        if (!cuentas.containsKey(c)) throw new IllegalArgumentException();
        if (cuentas.get(c) >= m) {
            if(alertas.containsKey(c)) alertas.get(c).add(m);
            else{
                Queue<Integer> aux = new LinkedList<>();
                aux.add(m);
                alertas.put(c,aux);
            }
            no_creadas.get(c).getRight().await();
            while(!permiso_minimo){}
            permiso_minimo = false;
        }
        mutex.leave();
        mutex.enter();
        desbloqueo_minimo();
        mutex.leave();
    }
}
