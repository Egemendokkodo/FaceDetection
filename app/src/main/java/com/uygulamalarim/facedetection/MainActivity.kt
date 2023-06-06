package com.uygulamalarim.facedetection

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONException
import org.json.JSONObject
import java.io.*


class MainActivity : AppCompatActivity() {


    // activity_main.xml dosyasında tanımladığımız bazı elemanlara burada global değişken olarak tanımladık.
    private lateinit var pick_from_gallery_button: Button // bu buton galeriden resim seçme butonudur.
    private lateinit var imageView: ImageView // seçtiğimiz resmi bu ImageView ögesine ekliyoruz.

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pick_from_gallery_button = findViewById(R.id.pick_from_galery_button) // galeriden resim seç butonunu initialize ediyoruz.
        imageView = findViewById(R.id.imageView) // aynı şekilde imageview'u da initialize ediyoruz

        var progressBar:ProgressBar=findViewById(R.id.progressBar) // aynı şekilde progressbar'ı da initialize ediyoruz, bu progress bar yükleme yaparken döndürülen mavi göstergedir.
        progressBar.visibility=View.GONE // başta herhangi bir yükleme yapmadığımız için progressbar'ı yokediyoruz.

        var infoContainer:LinearLayout=findViewById(R.id.infoContainer) // başta herhangi bir yükleme yapmadığımız için yazıların olduğu yeri komple yok ediyoruz.
        infoContainer.visibility=View.GONE // başta herhangi bir yükleme yapmadığımız için progressbar'ı yokediyoruz.

        pick_from_gallery_button.setOnClickListener {
            // burada galeriden resim seç butonuna tıkladığımızda çalışan 2 kod vardır bu kodlar-
            // galeri intentini açar. bu galeri intentinde eğer resim seçersek bizi onActivityResult fonksiyonuna gönderir.
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // burada eğer resmi seçersek sonuç(result) olarak bu fonksiyon çağrılır.


        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri = data.data!! //burada bir önceki aktiviteden(galeri sayfasından) gelen resmi tutuyoruz.


            //BU KISIMDA GELEN RESİM FORMATI NE OLURSA OLSUN ONU .JPG RESİM DOSYASINA ÇEVİRİYORUZ
            val inputStream = contentResolver.openInputStream(selectedImageUri)
            val file = File(cacheDir, "image.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            //------------------------------------------------------------------------------------

            imageView.setImageURI(selectedImageUri) // seçtiğimiz resmi kullanıcıya gösterelim.
            val requestBody = MultipartBody.Builder()//bu kısımda apiye bir POST işlemi uyguluyoruz. bu post işleminde gönderilecek olan veri bizim resim verimizdir.
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image_input",
                    "image.jpg",
                    RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
                )
                .build()

            val request = Request.Builder()
                .url("https://face-recognition18.p.rapidapi.com/recognize_face")
                .post(requestBody)
                .addHeader("content-type", "multipart/form-data; boundary=---011000010111000001101001")
                .addHeader("X-RapidAPI-Key", "53715be364msh739fbd75fe56245p12bfc4jsn28566c0c7d6f") // TODO:: ÖNEMLİ! PROJEYİ TESLİM ETMEDEN ÖNEC BU KISIMA APİ KEY İNİZİ EKLEMELİSİN.
                .addHeader("X-RapidAPI-Host", "face-recognition18.p.rapidapi.com")
                .build()

            val client = OkHttpClient() // okhttp kütüphanesini çağırıyoruz.
            val call = client.newCall(request)


            // progressbarla infocontainer ı tekrardan initialize ettim.
            // bunu yapmamın sebebi, bu fonksiyonun ve ana fonksiyonun farklı zamanlarda çağrılması.
            var progressBar: ProgressBar = findViewById(R.id.progressBar)
            progressBar.visibility = View.VISIBLE
            var infoContainer:LinearLayout=findViewById(R.id.infoContainer)
            infoContainer.visibility=View.GONE
            //-----------------------------------------------------------------
            call.enqueue(object : Callback { // burada bir call fonksiyonu var, bu fonksiyon async taskları yönetmemizi sağlar.
                override fun onFailure(call: Call, e: IOException) {
                    // hata durumunda yapılacaklar
                }

                override fun onResponse(call: Call, response: Response) {

                    // eğer işlem başarılıysa buraya gireriz.


                    val responseBody = response.body?.string() // apiden gelen yanıt
                    Log.d("RESPONSEBODY:::",responseBody.toString())



                    // sonucu yazdıracağımız textviewları initialize ettik.
                    var emotionTv:TextView=findViewById(R.id.emotionTv)
                    var emotionProbTv:TextView=findViewById(R.id.emotionProbTv)
                    var genderProbTv:TextView=findViewById(R.id.genderProbTv)
                    var genderTv:TextView=findViewById(R.id.genderTv)
                    var isRealFaceTv:TextView=findViewById(R.id.isRealFaceTv)
                    //-------------------------------------------------------

                    val jsonObject = JSONObject(responseBody) // gelen yanıtı bir json nesnesine dönüştürelim.
                    val status = jsonObject.getString("status") // gelen veriden status alan adını ayıklayalım.
                    // bunu yapmamızın sebebi, eğer api isteği başarısız olursa gerekli adımları uygulamak.
                    //Log.d("STATUS:", status.toString())
                    val statusFailMidir: Boolean = status.toString() == "fail" // status fail midir?

                    if (statusFailMidir) { // eğer status fail ise burada hata verir
                        isRealFaceTv.post {
                            isRealFaceTv.setTextColor(Color.RED)
                            isRealFaceTv.text = "Fotoğrafta gerçek bir insan yüzü yok!"
                            progressBar.visibility = View.GONE
                            infoContainer.visibility = View.VISIBLE
                            genderTv.visibility=View.GONE
                            genderProbTv.visibility=View.GONE
                            emotionTv.visibility=View.GONE
                            emotionProbTv.visibility=View.GONE
                        }
                    } else { // status fail değil ise apiden gelen diğer alan adlarını ayıklar. bunlara null-check yaptım.
                        val imageFileName = jsonObject.optString("image_file_name", "")
                        val recognitionResultArray = jsonObject.optJSONArray("recognition_result")
                        val recognitionUidResult = recognitionResultArray?.optJSONObject(0)?.optString("recognition_uidresult", "")
                        val recognitionConfidence = recognitionResultArray?.optJSONObject(0)?.optString("recognition_confidence", "")
                        val gender = recognitionResultArray?.optJSONObject(0)?.optString("gender", "")
                        val genderProbability = recognitionResultArray?.optJSONObject(0)?.optDouble("gender_probability", 0.0)?.times(100)
                        val emotion = recognitionResultArray?.optJSONObject(0)?.optString("emotion", "")
                        val emotionProbability = recognitionResultArray?.optJSONObject(0)?.optDouble("emotion_probability", 0.0)?.times(100)

                        val livenessObject = recognitionResultArray?.optJSONObject(0)?.optJSONObject("liveness")
                        val isRealFace = livenessObject?.optBoolean("is_real_face", false)
                        val livenessProbability = livenessObject?.optDouble("liveness_probability", 0.0)

                        isRealFaceTv.post { // fotoğraftaki gyüzü algılayamadığında
                            if (!isRealFace!!) {
                                isRealFaceTv.setTextColor(Color.RED)
                                isRealFaceTv.text = "Fotoğrafta gerçek bir insan yüzü yok!"
                                progressBar.visibility = View.GONE
                                infoContainer.visibility = View.VISIBLE
                            } else { // fotoğraftaki yüzü algıladığında
                                genderTv.visibility=View.VISIBLE
                                genderProbTv.visibility=View.VISIBLE
                                emotionTv.visibility=View.VISIBLE
                                emotionProbTv.visibility=View.VISIBLE
                                isRealFaceTv.text = "Yüz algılandı."
                                isRealFaceTv.setTextColor(Color.GREEN)
                                genderTv.text = "Cinsiyet: ${gender.toString().toUpperCase()}"
                                genderProbTv.text = "Cinsiyet Doğruluk Olasılığı: %${genderProbability.toString()}"
                                emotionTv.text = "Duygu: ${emotion.toString().toUpperCase()}"
                                emotionProbTv.text = "Duygu Doğruluk Olasılığı: %${emotionProbability.toString()}"
                                progressBar.visibility = View.GONE
                                infoContainer.visibility = View.VISIBLE
                            }
                        }
                    }





                }
            })
        }
    }




}
// apiden gelen örnek veri:
// {"status":"success","image_file_name":"image.jpg","recognition_result":[{"recognition_uidresult":"unknown","recognition_confidence":"0.0","recognition_otheruids":[],"bbox":{"x1":0.20484,"y1":0.0,"x2":0.62258,"y2":0.82},"liveness":{"is_real_face":true,"liveness_probability":0.88304},"age":21.95,"gender":"female","gender_probability":0.98409,"emotion":"happy","emotion_probability":0.99875,"wear_facemask":false}],"latency_ms":141.62}



