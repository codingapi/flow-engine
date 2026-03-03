# Groovy Script 脚本设计规范

## 脚本的实例写法如下
```
def run(request){
    return "Hello, ${request.name}!"
}
```

## 脚本规范
request对象，根据脚本的不同，起传递的request对象也不同。
return语句，return语句根据脚本的不同，起返回的对象也不同。

## 开发规范
为了让脚本可以更好的呈现和使用，脚本的配置分为两种模式，一种是可视化配置模式，一种是代码配置模式。

* 代码配置模式
代码配置模式的脚本中，将会通过注释的方式添加一行@CUSTOM_SCRIPT，来标识这是一个自定义脚本，这样在编辑器中就会以代码的形式展示出来。
```
// @CUSTOM_SCRIPT
def run(request){
    return "Hello, ${request.name}!"
}
```

* 可视化配置模式
可视化配置模式的脚本中，没有@CUSTOM_SCRIPT的注释标识，这样在编辑器中就会以可视化的形式展示出来。
```
def run(request){
    return "Hello, ${request.name}!"
}
```

## 脚本展示标题
为了让脚本在在展示时可以更好的展示脚本的作用，所以在脚本中支持通过@SCRIPT_TITLE的注释来标识脚本的展示标题，这样在编辑器中就会以这个标题来展示脚本。 

```
// @SCRIPT_TITLE 这是一个示例脚本
def run(request){
    return "Hello, ${request.name}!"
}
```
上述的脚本在编辑器中就会以“这是一个示例脚本”来展示，而不是以代码的方式来展示。