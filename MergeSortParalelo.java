import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class MergeSortParalelo {

    static Scanner sc = new Scanner(System.in);
    static volatile boolean faltouMemoria = false;

    // rode com: java -Xmx4G MergeSortParalelo
    public static void main(String[] args) {
        try {
            // ===== Bloco 1: Tamanho limite do vetor =====
            int limite = calcularLimite();

            // ===== Bloco 2: Entrada de dados =====
            int tamanho = lerInt("\nQuantos elementos no vetor? (máx. " + String.format("%,d", limite) + "): ", 1, limite);
            int opcao = lerInt("1 - Digitar os valores | 2 - Gerar aleatoriamente: ", 1, 2);

            byte[] vetor = new byte[tamanho];
            if (opcao == 1) {
                for (int i = 0; i < tamanho; i++) {
                    vetor[i] = (byte) lerInt("Elemento " + i + " (-128 a 127): ", -128, 127);
                }
            } else {
                System.out.println("[LOG] Gerando valores aleatórios...");
                new Random().nextBytes(vetor); // valores de -128 a 127
            }

            // ===== Bloco 3: Ordenação =====
            int modo = lerInt("Modo: 1 - Paralelo | 2 - Sequencial | 3 - Ambos (comparar): ", 1, 3);
            byte[] vetorFinal = null;
            long tempoSeq = -1;
            long tempoPar = -1;

            if (modo == 2 || modo == 3) {
                // no modo 3 ordena uma cópia, para o paralelo receber o vetor original
                byte[] copia = (modo == 3) ? vetor.clone() : vetor;
                System.out.println("[LOG] Ordenando sequencialmente...");
                long inicio = System.nanoTime();
                new Ordenadora(copia).run(); // run() direto: executa na thread main, sem paralelismo
                tempoSeq = System.nanoTime() - inicio;
                vetorFinal = copia;
            }

            if (modo == 1 || modo == 3) {
                int threads = Runtime.getRuntime().availableProcessors();
                System.out.println("[LOG] Ordenando em paralelo com " + threads + " threads...");
                long inicio = System.nanoTime();
                byte[] ordenado = ordenarParalelo(vetor, threads);
                tempoPar = System.nanoTime() - inicio;
                if (faltouMemoria) {
                    System.out.println("[ERRO] Faltou memória durante a ordenação paralela. Tente um vetor menor ou aumente o -Xmx.");
                    return;
                }
                vetorFinal = ordenado;
            }

            // ===== Bloco 4: Resultado =====
            imprimirVetor(vetorFinal);
            System.out.println("Vetor ordenado corretamente? " + (estaOrdenado(vetorFinal) ? "SIM" : "NÃO"));
            if (tempoSeq >= 0) System.out.printf("Tempo sequencial: %.3f ms%n", tempoSeq / 1e6);
            if (tempoPar >= 0) System.out.printf("Tempo paralelo:   %.3f ms%n", tempoPar / 1e6);
            if (tempoSeq > 0 && tempoPar > 0) System.out.printf("Speedup: %.2fx%n", (double) tempoSeq / tempoPar);

        } catch (OutOfMemoryError e) {
            System.out.println("[ERRO] Memória insuficiente. Tente um vetor menor ou aumente o -Xmx.");
        } catch (InterruptedException e) {
            System.out.println("[ERRO] Execução interrompida.");
        } catch (IllegalStateException e) {
            System.out.println("\n[ERRO] " + e.getMessage());
        }
    }

    // Divide o vetor em pedaços, ordena cada um numa Ordenadora e junta os pedaços com Misturadoras
    static byte[] ordenarParalelo(byte[] vetor, int threads) throws InterruptedException {
        int partes = Math.max(1, Math.min(threads, vetor.length));

        // Fase 1: cada Ordenadora ordena o seu pedaço ao mesmo tempo
        Ordenadora[] ordenadoras = new Ordenadora[partes];
        for (int p = 0; p < partes; p++) {
            int ini = (int) ((long) vetor.length * p / partes);
            int fim = (int) ((long) vetor.length * (p + 1) / partes);
            ordenadoras[p] = new Ordenadora(Arrays.copyOfRange(vetor, ini, fim));
            ordenadoras[p].setUncaughtExceptionHandler((t, e) -> faltouMemoria = true);
            ordenadoras[p].start();
        }

        byte[][] pedacos = new byte[partes][];
        for (int p = 0; p < partes; p++) {
            ordenadoras[p].join();
            pedacos[p] = ordenadoras[p].getPedaco();
        }
        if (faltouMemoria) return null;

        // Fase 2: junta os pedaços dois a dois, em rodadas, até sobrar um só
        while (pedacos.length > 1) {
            int pares = pedacos.length / 2;
            Misturadora[] misturadoras = new Misturadora[pares];
            for (int p = 0; p < pares; p++) {
                misturadoras[p] = new Misturadora(pedacos[2 * p], pedacos[2 * p + 1]);
                misturadoras[p].setUncaughtExceptionHandler((t, e) -> faltouMemoria = true);
                misturadoras[p].start();
            }

            byte[][] proximos = new byte[(pedacos.length + 1) / 2][];
            for (int p = 0; p < pares; p++) {
                misturadoras[p].join();
                proximos[p] = misturadoras[p].getResultado();
            }
            if (faltouMemoria) return null;

            // se a quantidade for ímpar, o último pedaço passa direto para a próxima rodada
            if (pedacos.length % 2 == 1) proximos[pares] = pedacos[pedacos.length - 1];
            pedacos = proximos;
        }
        return pedacos[0];
    }

    // Lê um inteiro entre min e max, repetindo até o usuário digitar um valor válido
    static int lerInt(String mensagem, int min, int max) {
        while (true) {
            System.out.print(mensagem);
            if (!sc.hasNextLine()) {
                throw new IllegalStateException("Entrada encerrada.");
            }
            String linha = sc.nextLine().trim().replace(".", "").replace(",", "");
            try {
                int valor = Integer.parseInt(linha);
                if (valor >= min && valor <= max) return valor;
                System.out.printf("Valor fora do intervalo (%,d a %,d).%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Digite um número inteiro.");
            }
        }
    }

    static boolean estaOrdenado(byte[] v) {
        for (int i = 1; i < v.length; i++) {
            if (v[i - 1] > v[i]) return false;
        }
        return true;
    }

    // Vetores pequenos são impressos inteiros; grandes, só o começo e o fim
    static void imprimirVetor(byte[] v) {
        int mostrar = 10;
        if (v.length <= 2 * mostrar) {
            System.out.println("\nVetor ordenado: " + Arrays.toString(v));
            return;
        }
        StringBuilder sb = new StringBuilder("\nVetor ordenado: [");
        for (int i = 0; i < mostrar; i++) sb.append(v[i]).append(", ");
        sb.append("... ");
        for (int i = v.length - mostrar; i < v.length; i++) {
            sb.append(", ").append(v[i]);
        }
        sb.append("]");
        System.out.println(sb);
    }

    static int calcularLimite() {
        System.out.println("Estimando o maior tamanho possível de vetor em Java...");
        long inicio = System.currentTimeMillis();

        int tamanho = 1_000_000; // começa com 1 milhão
        int ultimoBemSucedido = 0;

        while (true) {
            try {
                byte[] vetor = new byte[tamanho];
                ultimoBemSucedido = tamanho;
                vetor = null; // libera
                System.gc();

                System.out.printf("Alocado com sucesso: %,d elementos%n", ultimoBemSucedido);

                // aumenta o tamanho em 50% para a próxima tentativa
                if (tamanho > Integer.MAX_VALUE / 3 * 2) break;

                tamanho /= 2;
                tamanho *= 3;
            } catch (OutOfMemoryError e) {
                System.out.printf("Falhou em %,d elementos%n", tamanho);
                break;
            }
        }

        long fim = System.currentTimeMillis();
        System.out.println("\nMaior vetor que coube (aproximadamente): " +
                String.format("%,d", ultimoBemSucedido));
        System.out.printf("Memória estimada: %.2f MB%n",
                ultimoBemSucedido * 1.0 / (1024 * 1024));
        System.out.printf("Tempo total: %.2f segundos%n", (fim - inicio) / 1000.0);

        return ultimoBemSucedido;
    }
}
