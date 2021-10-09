import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;

public class RegistroDeServicos {
    private String zNodeAtual;
    private List<String> enderecos;
    public static final String REGISTRO_ZNODE = "/registro_de_servicos";
    private final ZooKeeper zooKeeper;

    public RegistroDeServicos (ZooKeeper zooKeeper){
        this.zooKeeper = zooKeeper;
        try{
            if (this.zooKeeper.exists(REGISTRO_ZNODE, false) == null){
                this.zooKeeper.create(
                        REGISTRO_ZNODE,
                        new byte[]{},
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
            }
        }
        catch (InterruptedException | KeeperException e){
            e.printStackTrace();
        }
    }

    public void registroJuntoAoCluster (String configuracoes)
                    throws InterruptedException, KeeperException{
        if (this.zNodeAtual != null){
            System.out.printf ("Processo [%s] já registrado\n", this.zNodeAtual);
        }else{
            this.zNodeAtual = zooKeeper.create(
                    String.format("%s/p_", REGISTRO_ZNODE),
                    configuracoes.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL
            );
            System.out.printf("Registro realizado: [%s]\n", this.zNodeAtual);
        }

    }

    public void atualizarEnderecos () throws InterruptedException, KeeperException{
        synchronized (this){
            List <String> znodes = zooKeeper.getChildren(REGISTRO_ZNODE, registroDeServicosWatcher);
            for (String znode: znodes){
                Stat stat = null;
                if((stat = zooKeeper.exists(
                        String.format("%s/%s", REGISTRO_ZNODE, znode),
                        false
                )) != null){
                    enderecos.add (new String(zooKeeper.getData(
                            String.format("%s/%s", REGISTRO_ZNODE, znode),
                            false,
                            stat
                    )));
                }
            }
            this.enderecos = Collections.unmodifiableList(this.enderecos);
            System.out.printf ("Endereços atualmente disponíveis: %s\n", this.enderecos.toString());
        }
    }

    private Watcher registroDeServicosWatcher = (event) -> {
        try {
            atualizarEnderecos();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    };

    public void registrarseParaReceberAtualizacoes (){
        try {
            atualizarEnderecos();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    public List <String> obterTodosOsEnderecos () throws InterruptedException, KeeperException {
        synchronized (this){
            if (this.enderecos == null)
                atualizarEnderecos();
            return this.enderecos;
        }
    }

    public void removerDoCluster () throws InterruptedException, KeeperException {
        if (zNodeAtual != null && zooKeeper.exists(zNodeAtual, false) != null){
            zooKeeper.delete(zNodeAtual, -1);
        }
    }
}
