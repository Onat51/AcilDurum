## Admin Şifresini Firebase'e Taşıma

Constants.java'daki hardcode şifre kaldırıldı.
Artık şifre Firebase Realtime DB'de şu yolda saklanıyor:

  admin_settings/password_hash  →  SHA-256 hash değeri

### Tek seferlik kurulum (Firebase Console'dan manuel):

1. Firebase Console → Realtime Database → veri ekle:

```json
{
  "admin_settings": {
    "password_hash": "<SHA-256-HASH-BURAYA>"
  }
}
```

2. SHA-256 hash üretmek için terminalde:

```bash
echo -n "YeniŞifreniz" | sha256sum
# Veya Python ile:
python3 -c "import hashlib; print(hashlib.sha256(b'YeniŞifreniz').hexdigest())"
```

3. Üretilen hash değerini Firebase Console'a yapıştır.

### Önemli notlar:
- Eski şifre artık kodda YOK.
- Şifre değiştirmek için Firebase Console'dan hash'i güncellemek yeterli,
  uygulama güncellemesi gerekmez.
- Production'da BCrypt (bcrypt-java kütüphanesi) kullanılması önerilir.
  Şu an SHA-256 kullanılıyor — saldırı yüzeyi düşük (şifreler client'ta değil,
  Firebase güvenlik kurallarıyla korunacak) ama yine de BCrypt daha güvenli.

### Firebase Güvenlik Kuralı (admin_settings okuma koruması):
```json
{
  "rules": {
    "admin_settings": {
      ".read": false,
      ".write": false
    }
  }
}
```
Bu kural ile admin_settings düğümüne client-side erişim kapatılır.
Şifre doğrulaması için özel Cloud Function yazılması en güvenli yöntemdir.
