# Guia Completo: Tradutor de Tela com Bolinha Flutuante (Android)

Este aplicativo utiliza duas tecnologias oficiais e 100% nativas do Android:
1. **Google ML Kit Text Recognition (OCR)**: Lê as letras da tela diretamente no processador do celular.
2. **Google ML Kit Translate**: Traduz do Inglês para o Português (Brasil) offline, sem gastar dados e sem precisar de nenhuma chave de API.

---

### Por que NÃO é considerado Hack e NÃO dá Ban em Jogos?
- **Não altera arquivos do jogo:** O app nunca toca no APK original ou nos arquivos `.obb` / `.data`.
- **Não injeta código na memória RAM:** Ao contrário de ferramentas de trapaça (GameGuardian, CheatEngine), este app apenas captura um quadro da tela e desenha uma sobreposição por cima (`SYSTEM_ALERT_WINDOW`).
- **É o mesmo princípio do Discord Overlay, gravadores de gameplay (OBS, Mobizen) e o próprio Google Tradutor (Tap to Translate).**

---

### Como abrir no Android Studio:
1. Abra o **Android Studio** e clique em **Open...**.
2. Selecione a pasta onde você descompactou os arquivos deste projeto.
3. Aguarde o Gradle sincronizar as dependências (`build.gradle.kts`).
4. Conecte seu celular Android via cabo USB (com Depuração USB ativada) ou inicie um Emulador.
5. Clique no botão verde de **Run (Play)** no topo.

### Como Gerar o .APK direto na Nuvem (sem Android Studio no PC):
1. Suba este projeto para um repositório no seu GitHub.
2. O arquivo `.github/workflows/build-apk.yml` será executado automaticamente pelo GitHub Actions.
3. Em 2 minutos, você poderá baixar o `app-debug.apk` pronto para instalar no celular!
