<html>
<head>
    <meta charset="utf-8">
    <title>HTML5大文件分片上传示例</title>
    <script src="http://cdn.bootcss.com/jquery/1.12.4/jquery.min.js"></script>
    <script src="../static/js/md5.js"></script>
    <script type="text/javascript">
        var shardSize = 1 * 1024 * 1024;    //以1MB为一个分片
        var dataBegin;  //开始时间
        var dataEnd;    //结束时间
        let success = 0;
        var page = {
            init: function () {
                $("#upload").click(function () {
                    dataBegin = new Date();
                    var file = $("#file")[0].files[0];  //文件对象
                    isUpload(file);
                });
            }
        };
        $(function () {
            page.init();
        });
        function isUpload (file) {
            //构造一个表单，FormData是HTML5新增的
            var form = new FormData();
            var r = new FileReader();
            r.readAsBinaryString(file);
            $(r).load(function(e){
                var blob = e.target.result;
                var md5 = hex_md5(blob);
                form.append("md5", md5);
                form.append("name", file.name);
                form.append("size", file.size);
                //Ajax提交
                $.ajax({
                    url: "isUpload",
                    type: "POST",
                    data: form,
                    async: true,        //异步
                    processData: false,  //很重要，告诉jquery不要对form进行处理
                    contentType: false,  //很重要，指定为false才能形成正确的Content-Type
                    success: function(data){
                        var uuid = data.fileId;
                        if (data.flag == "0" || data.flag == "1") {
                            //没有上传过文件
                            //realUpload(file,uuid,md5,data.date);
                            let size = file.size;
                            let shardCount = Math.ceil(size / shardSize);  //总片数
                            for(let k=1;k<=shardCount;k++){
                                let partMd5 = '';
                                let start = (k-1) * shardSize;
                                let end = Math.min(size, start + shardSize);
                                let data = file.slice(start, end);
                                let r = new FileReader();
                                r.readAsBinaryString(data);
                                $(r).load(function (e) {
                                    var bolb = e.target.result;
                                    partMd5 = hex_md5(bolb);
                                })
                                checkPartFile(file,uuid,partMd5,shardCount,k,md5)
                            }

                        } else if(data.flag == "2") {
                            //文件已经上传过
                            alert("文件已经上传过,秒传了！！");
                        }
                    },error: function(XMLHttpRequest, textStatus, errorThrown) {
                        alert("服务器出错!");
                    }
                })
            })
        };

        /**
         * 检查分片文件
         */
        function checkPartFile(file,uuid,partMd5,total,index,md5) {
            let form = new FormData();
            form.append("uuid", uuid);
            form.append("partMd5", partMd5);
            form.append("name", file.name);
            form.append("size", file.size);
            form.append("index", index);  //当前是第几片
            $.ajax({
                url: "checkPartFile",
                type: "POST",
                data: form,
                async: false,        //异步
                processData: false,  //很重要，告诉jquery不要对form进行处理
                contentType: false,  //很重要，指定为false才能形成正确的Content-Type
                success: function (data) {
                    var fileuuid = data.fileId;
                    var flag = data.flag;
                    //切片文件已经上传成功
                    if (flag == 1){
                        console.log('切片文件已经上传成功')
                    }else {
                        realUpload1(file,uuid,md5,index);
                    }
                }, error: function (XMLHttpRequest, textStatus, errorThrown) {
                    alert("服务器出错!");
                }
            });
        }

        function assembleFiles(fileuuid, shardCount) {
            $.ajax({
                url: "assembleFiles?uuid="+fileuuid+"&total="+shardCount,
                type: "GET",
                async: true,        //异步
                contentType: false,  //很重要，指定为false才能形成正确的Content-Type
                success: function (data) {
                    console.log('文件组装完成');
                }, error: function (XMLHttpRequest, textStatus, errorThrown) {
                    alert("服务器出错!");
                }
            });
        }

        function realUpload1(file, uuid, md5,index) {

            let name = file.name;
            let size = file.size;
            let shardCount = Math.ceil(size / shardSize);  //总片数
            //计算每一片的起始与结束位置
            let start = (index-1) * shardSize;
            let end = Math.min(size, start + shardSize);
            //构造一个表单，FormData是HTML5新增的
            let form = new FormData();
            form.append("action", "upload");  //直接上传分片
            form.append("data", file.slice(start, end));  //slice方法用于切出文件的一部分
            form.append("uuid", uuid);
            form.append("md5", md5);
            form.append("name", name);
            form.append("size", size);
            form.append("total", shardCount);  //总片数
            form.append("index", index);        //当前是第几片

            //按大小切割文件段　　
            var data = file.slice(start, end);
            var r = new FileReader();
            r.readAsBinaryString(data);
            $(r).load(function (e) {
                var bolb = e.target.result;
                var partMd5 = hex_md5(bolb);
                form.append("partMd5", partMd5);
                //Ajax提交
                $.ajax({
                    url: "upload",
                    type: "POST",
                    data: form,
                    async: false,        //异步
                    processData: false,  //很重要，告诉jquery不要对form进行处理
                    contentType: false,  //很重要，指定为false才能形成正确的Content-Type
                    success: function (data) {
                        var fileuuid = data.fileId;
                        var flag = data.flag;
                        if (flag == 2){
                            console.log('文件传输完成');
                        }else {
                            console.log('第'+index+'片文件传输完成');
                        }
                        success++;
                        $("#output").text((((0.0+success)/(shardCount*1.0)*100).toFixed(2))+'%');
                        console.log('success '+success+' total '+shardCount);
                        if (success == shardCount){
                            console.log('所有分片文件传输完成');
                            assembleFiles(fileuuid,shardCount);
                        }
                    }, error: function (XMLHttpRequest, textStatus, errorThrown) {
                        alert("服务器出错!");
                    }
                });
            })
        }

        
    </script>
</head>

<body>

<input type="file" id="file" />
<button id="upload">上传</button>
<br/><br/>
<span style="font-size:16px">上传进度：</span><span id="output" style="font-size:16px"></span>
<span id="useTime" style="font-size:16px;margin-left:20px;">上传时间：</span>
<span id="uuid" style="font-size:16px;margin-left:20px;">文件ID：</span>
<br/><br/>
<span id="param" style="font-size:16px">上传过程：</span>

</body>
</html>