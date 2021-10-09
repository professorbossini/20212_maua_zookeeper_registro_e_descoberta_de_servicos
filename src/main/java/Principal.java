import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class Principal {

    public static final String HOST = "localhost";
    public static final String PORTA = "2181";
    public static final int TIMEOUT = 5000;
    private ZooKeeper zooKeeper;

    public ZooKeeper conectar () throws IOException{
        this.zooKeeper = new ZooKeeper(
                String.format("%s:%s", HOST, PORTA),
                TIMEOUT,
                (evento) -> {
                    switch (evento.getType()){
                        case None:
                            if (evento.getState() == Watcher.Event.KeeperState.SyncConnected){
                                System.out.println("Conectou");
                            }
                            else{
                                synchronized (zooKeeper){
                                    System.out.println ("Desconectou");
                                    zooKeeper.notify();
                                }
                            }
                    }
                }
        );
        return this.zooKeeper;
    }

    public void fechar () throws InterruptedException{
        this.zooKeeper.close();
    }
    public void executar () throws InterruptedException{
        synchronized (zooKeeper){
            zooKeeper.wait();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        int porta;
        try{
            porta = Integer.parseInt(args[0]);
        }
        catch (Exception e){
            porta = 10000;
        }
        Principal principal = new Principal();
        principal.conectar();
        RegistroDeServicos registroDeServicos = new RegistroDeServicos(principal.zooKeeper);
        EleicaoCallback eleicaoCallback = new EleicaoCallbackImpl(registroDeServicos, porta);
        EleicaoDeLider eleicaoDeLider = new EleicaoDeLider(principal.zooKeeper, eleicaoCallback);
        eleicaoDeLider.realizarCandidatura();
        eleicaoDeLider.eleicaoEReeleicaoDeLider();
        principal.executar();
        principal.fechar();
    }


}
