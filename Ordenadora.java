class Ordenadora extends Thread { // Processo independente. Várias Ordenadoras podem ser executados em paralelo
    private byte[] pedaco;

    public Ordenadora(byte[] pedaco) { // Construtor
        this.pedaco = pedaco;
    }

    public void run() {
        mergeSort(pedaco, 0, pedaco.length - 1);
    }

    public byte[] getPedaco() { // Getter
        return this.pedaco;
    }

    //Divisao Recursiva
    private void mergeSort(byte[] v, int inicio, int fim) {
        if (inicio >= fim) return; // Condicao de parada
        int meio = inicio + (fim - inicio) / 2; // Pega o pedaço e divide (sem estourar o int)
        mergeSort(v, inicio, meio); // Ordena esq
        mergeSort(v, meio + 1, fim); // Ordena dir
        intercalar(v, inicio, meio, fim); // Costurar as metades de volta
    }

    private void intercalar(byte[] v, int inicio, int meio, int fim) {
        byte[] aux = new byte [fim - inicio + 1]; // Vetor temporario com tamanho exato da soma
        int i = inicio; // Inicio esq
        int j = meio + 1; // Inicio dir
        int k = 0;

        // Comparar qual é menor entre i e j e colocar no vetor auxiliar
        // esse ponteiro avança, garantindo que seja preenchido em ordem crescente
        while (i <= meio && j <= fim) {
            if (v[i] <= v[j]) {
                aux[k] = v[i];
                i++;
            } else {
                aux[k] = v[j];
                j++;
            }
            k++;
        }

        // Garante que se uma metade acabar antes, ela descarrega oq sobrou da outra
        // no final de aux
        while (i <= meio) {
            aux[k] = v[i];
            i++;
            k++;
        }

        // Garante que se uma metade acabar antes, ela descarrega oq sobrou da outra
        // no final de aux
        while (j <= fim) {
            aux[k] = v[j];
            j++;
            k++;
        }

        // Pega tudo que foi organizado em aux e copia de volta para o vetor original v
        for (k = 0; k < aux.length; k++) {
            v[inicio + k] = aux[k];
        }
    }
}