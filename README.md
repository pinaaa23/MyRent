# MyRent - Aplikasi Manajemen Sewa Barang

MyRent adalah aplikasi Android modern berbasis Kotlin dan Jetpack Compose (Material 3) dengan integrasi Firebase Authentication dan Cloud Firestore untuk pengelolaan katalog barang, transaksi sewa, pembayaran QRIS/Tunai, cetak nota thermal, serta reminder WhatsApp otomatis.

## Fitur Utama
- **Autentikasi & Verifikasi Email**: Registrasi dan Login dengan verifikasi email Firebase Auth.
- **Multi-Cabang & Kategori**: Pengelolaan cabang toko dan kategori barang dinamis.
- **Katalog & Manajemen Stok**: Manajemen stok barang, harga sewa per unit, dan status ketersediaan.
- **Alur Transaksi Sewa 2 Langkah**:
  1. Pilih Barang dari Katalog (Grid 2 Kolom dengan Foto & Status Stok).
  2. Input Data Penyewa, Durasi Sewa, Jumlah Unit, & Kalkulasi Otomatis Biaya & DP.
- **Pembayaran QRIS & Tunai Full-Screen**: Tampilan QR Code QRIS penuh presisi untuk kemudahan scan konsumen.
- **Nota Transaksi (Struk Thermal Kasir)**: Cetak nota via printer thermal/PDF dan Kirim Invoice Nota via WhatsApp.
- **Dashboard & Follow-Up Reminders**:
  - Statistik Sewa Berjalan, Jatuh Tempo Hari Ini, & Omzet Hari Ini (Uang Masuk Riil).
  - Reminder Follow-Up H-1 dan Hari H via WhatsApp dengan local notification.
