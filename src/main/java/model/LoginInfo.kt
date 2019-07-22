package model

class Login(password: String) {
    lateinit var id: AutoLogin
    var email: String? = null
    var nickname: String? = null
    var ip: String?=null
}


class AutoLogin(var a: Int?){

}