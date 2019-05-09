引用了本地仓库
执行以下步骤提交版本:
1.执行 gradle uploadArchives
2.引用仓库增加:
repositories {
    flatDir {
        dirs 'libs'
    }
    maven { url "http://localhost" }
}
3.
dependencies增加依赖
compile 'com.edusoho.videoplayer:es-vidoplayer:1.0-SNAPSHOT@aar'


新增:
上传本地,执行
gradle uploadArchives


上传服务器,执行
gradle uploadRemote