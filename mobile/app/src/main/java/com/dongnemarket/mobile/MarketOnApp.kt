package com.dongnemarket.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 앱 프로세스가 뜰 때 Activity보다 먼저 딱 한 번 생성되는 클래스.
 *
 * @HiltAndroidApp 이 붙으면 Hilt가 컴파일 시점에 "의존성 컨테이너"(Application 범위)를
 * 만들어 준다. Spring의 ApplicationContext가 서버 부팅 때 생성되는 것과 같은 자리.
 * 이 컨테이너에서 Retrofit·OkHttp·DataStore 같은 싱글톤이 만들어져 필요한 곳에 주입된다.
 *
 * AndroidManifest.xml 의 android:name=".MarketOnApp" 로 등록해야 실제로 사용된다.
 */
@HiltAndroidApp
class MarketOnApp : Application()
