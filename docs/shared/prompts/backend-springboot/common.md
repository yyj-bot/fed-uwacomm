# springboot项目基本开发规范
## 项目约束
### 基本定义
该项目为springboot 3.4.4版本的项目，配置有log4j，lombok等常用工具，使用mybatis连接数据库，基于junit执行单元测试

### 重构相关 
1.不需要在每次修改/重构以后保存总结文档，直接输出总结即可
2.确保接口文档的正确性，在每次重构前，最先修改的永远是接口文档，同时要保证接口文档的格式的正确性，查询参数和返回参数应该具有约束，数据结构定义，注释，且表明使用的是query还是json


### 基本开发相关
1.所有工具类(****util.java)应该保存在 common软件包的utils文件夹下
2.在service层的任何地方都不应该出现 Result.failure .....，所有错误返回都应该定义异常，然后直接抛出，交给全局异常处理器统一处理
3.全局异常处理器位于 backend-springboot\feduwacomm-common\src\main\java\com\feduwacomm\handler\GlobalExceptionHandler.java 下
4.所有entity数据库实体类,dto数据传输类,vo数据返回类都应该定义在pojo模块下
5.所有config类都应该存储于server软件包下
6.所有serverice/controller/mapper在修改或重构完都应该更新相应的单元测试，然后运行测试
