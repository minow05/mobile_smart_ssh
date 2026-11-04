package com.example.mobile_smart_ssh.ui.wizard

enum class WizardStage {
    INPUT,       // Wprowadzanie IP, usera i hasła
    RUNNING,     // Automatyczny proces wdrożenia (logi na żywo)
    COMPLETED,   // Pomyślnie zweryfikowano i zapisano
    FAILED       // Błąd połączenia / wdrożenia
}
