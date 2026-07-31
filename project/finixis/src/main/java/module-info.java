module com.sevatyres {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.media;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires java.desktop;

    // Database stack
    requires com.zaxxer.hikari;
    requires java.sql;
    requires com.h2database;

    // SLF4J (required by HikariCP)
    requires org.slf4j;

    // JavaMail for email service
    requires java.mail;

    // Apache PDFBox (automatic modules) for inventory PDF import
    requires org.apache.pdfbox;
    requires org.apache.pdfbox.io;
    requires org.apache.fontbox;

    opens com.sevatyres            to javafx.fxml;
    opens com.sevatyres.controller to javafx.fxml;
    opens com.sevatyres.viewmodel  to javafx.fxml;
    opens com.sevatyres.model      to javafx.base, javafx.fxml;

    // Open db package to H2 so it can instantiate our BalanceTrigger via reflection
    opens com.sevatyres.db to com.h2database;

    exports com.sevatyres;
    exports com.sevatyres.service;
}
