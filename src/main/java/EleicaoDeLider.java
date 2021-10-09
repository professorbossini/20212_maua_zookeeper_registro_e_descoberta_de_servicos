import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;

public class EleicaoDeLider {
    private static final String ELEICAO = "/eleicao";
    private String nomeDoZNodeDesseProcesso;
    private ZooKeeper zooKeeper;
    private EleicaoCallback eleicaoCallback;

    public EleicaoDeLider (ZooKeeper zooKeeper, EleicaoCallback eleicaoCallback){
        this(zooKeeper);
        this.eleicaoCallback = eleicaoCallback;
    }

    public EleicaoDeLider (ZooKeeper zooKeeper){
        this.zooKeeper = zooKeeper;
        //observe a condição de corrida
        //dois processos podem chamar o método exists ao mesmo tempo...
        //ambos podem tentar criar o ZNode..
        //se isso acontecer, capturamos a exceção lançada por create e simplesmente a
        //descartamos
        try{
            if (this.zooKeeper.exists(ELEICAO, false) == null)
                this.zooKeeper.create(ELEICAO, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
        }
        catch (InterruptedException | KeeperException e){
            e.printStackTrace();
        }
    }

    public void realizarCandidatura () throws InterruptedException, KeeperException {
        //o prefixo
        String prefixo = String.format("%s/cand_", ELEICAO);
        //parâmetros: prefixo, dados a serem armazenados no ZNode, lista de segurança e
        //tipo do ZNode
        String pathInteiro = zooKeeper.create(prefixo, new byte[]{},
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        //exibe o path inteiro
        System.out.println(pathInteiro);
        //armazena somente o nome do ZNode recém criado
        this.nomeDoZNodeDesseProcesso = pathInteiro.replace(String.format("%s/",
                ELEICAO), "");
    }

    public void eleicaoEReeleicaoDeLider() throws InterruptedException, KeeperException{
        //para armazenar a referência ao ZNode do predecessor
        Stat statPredecessor = null;
        //para poder usar fora da estrutura de repetição
        String nomePredecessor = "";
        do{
            //obter os filhos
            List<String> candidatos = zooKeeper.getChildren(ELEICAO, false);
            //ordenar
            Collections.sort(candidatos);
            //pegar o menor
            String oMenor = candidatos.get(0);
            //verificar se o atual é o menor
            //se for, declarar-se líder e encerrar por aqui
            if (oMenor.equals(nomeDoZNodeDesseProcesso)){
                System.out.printf ("Me chamo %s e sou o líder.\n", nomeDoZNodeDesseProcesso);
                eleicaoCallback.onEleitoLider();
                return;
            }
            //se chegou aqui, não é o líder, avisar
            System.out.printf("Me chamo %s e não sou o líder. O líder é o %s.\n", nomeDoZNodeDesseProcesso,
                    oMenor);
            //pegar o índice de seu predecessor na lista de candidatos
            int indicePredecessor = Collections.binarySearch(candidatos, nomeDoZNodeDesseProcesso) - 1;
            //pegar o nome de seu predecessor
            nomePredecessor = candidatos.get(indicePredecessor);
            //registrar um watch em seu predecessor (Qual método usar? getData, getChildren ou exists?)
            statPredecessor = zooKeeper.exists(
                    String.format("%s/%s", ELEICAO, nomePredecessor),
                    reeleicaoWatcher
            );
        }while (statPredecessor == null);
        //avisar qual ZNode este processo está observando
        eleicaoCallback.onIndicadoATrabalhador();
        System.out.printf ("Estou observando o %s\n", nomePredecessor);
    }

    //fazer com classe e instância, depois lambda
    private Watcher reeleicaoWatcher = (event) -> {
        try{
            switch (event.getType()){
                case NodeDeleted:
                    eleicaoEReeleicaoDeLider();
                    break;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    };



}