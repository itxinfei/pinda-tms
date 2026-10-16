# 心飞TMS 短视频推广 · 选题1《运费算错一年亏多少钱》分镜合成脚本
# 1080x1920 竖屏 / 30fps / 45秒 / 7个分镜（对应 docs/短视频推广脚本.md 选题1分镜表）
# 需以 work 目录为当前目录运行（滤镜内全部使用无盘符相对路径，避开 ffmpeg 转义坑）
$ErrorActionPreference = 'Stop'
$doc  = 'D:/MyCode/pinda-tms/docs'
$src  = "$doc/screenshots"
$work = 'D:/MyCode/pinda-tms/docs/promo-video/work'
$seg  = "$work/seg"
New-Item -ItemType Directory -Force -Path $seg | Out-Null

# 字体复制到 work 目录，滤镜里用相对文件名
Copy-Item -LiteralPath 'C:\Windows\Fonts\msyhbd.ttc' "$work/font_bold.ttc" -Force
Copy-Item -LiteralPath 'C:\Windows\Fonts\msyh.ttc'   "$work/font_reg.ttc"  -Force

$enc = @('-r','30','-c:v','libx264','-crf','20','-preset','veryfast','-c:a','aac','-b:a','64k','-ar','44100')

function Run($ffmpegArgs, $label) {
  Write-Host "==> $label"
  & ffmpeg -y -hide_banner -loglevel error @ffmpegArgs
  if ($LASTEXITCODE -ne 0) { throw "FAILED: $label" }
}
function WriteFilter($name, $body) {
  [System.IO.File]::WriteAllText("$work/$name", $body, (New-Object System.Text.UTF8Encoding($false)))
}
function Wm {
  "drawtext=fontfile='font_reg.ttc':textfile='wm.txt':fontcolor=white@0.85:fontsize=42:box=1:boxcolor=black@0.25:boxborderw=14:x=(w-text_w)/2:y=150"
}
function Fades([double]$d) {
  $o = $d - 0.3
  "fade=t=in:st=0:d=0.25,fade=t=out:st=${o}:d=0.3"
}
function CapText($file, $size, $y) {
  "drawtext=fontfile='font_bold.ttc':textfile='${file}':fontcolor=white:fontsize=${size}:borderw=4:bordercolor=black@0.7:x=(w-text_w)/2:y=${y}"
}

# ---------- 分镜1：0-3s 大屏GIF + 钩子大字 ----------
$f1 = "color=c=0x0e1a2b:s=1080x1920:r=30:d=3[b];[0:v]fps=30,scale=1080:-2:flags=lanczos[f];[b][f]overlay=(W-w)/2:(H-h)/2[bg];[bg]" +
  (CapText 'cap_hook.txt' 76 300) + ',' + (Wm) + ',' + (Fades 3) + ',format=yuv420p[v]'
WriteFilter 'f1.txt' $f1
$a = @('-stream_loop','-1','-i',"$src/demo-bigscreen.gif",
      '-f','lavfi','-t','3','-i','anullsrc=r=44100:cl=stereo',
      '-filter_complex_script','f1.txt',
      '-map','[v]','-map','1:a','-t','3') + $enc + @("$seg/s1.mp4")
Run $a 'seg1'

# ---------- 通用：截图分镜（模糊背景 + 清晰画面缓慢推近 + 底部字幕） ----------
function ShotSeg($idx, $png, $capfile, $dur) {
  $n = [int]($dur * 30)
  $fc = "[0:v]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,gblur=sigma=28,eq=brightness=-0.12[b];" +
    "[0:v]scale=1080:-2:flags=lanczos[cc];" +
    "[cc]zoompan=z='min(1+0.0006*on,1.12)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=${n}:s=1080x608:fps=30[z];" +
    "[b][z]overlay=0:(H-h)/2[bg];[bg]" +
    (CapText $capfile 64 1430) + ',' + (Fades $dur) + ',format=yuv420p[v]'
  WriteFilter "f$idx.txt" $fc
  $a = @('-i',"$src/$png",
        '-f','lavfi','-t',"$dur",'-i','anullsrc=r=44100:cl=stereo',
        '-filter_complex_script',"f$idx.txt",
        '-map','[v]','-map','1:a','-t',"$dur") + $enc + @("$seg/s$idx.mp4")
  Run $a "seg$idx $png"
}

ShotSeg 2 'admin-order.png'     'cap_k2.txt' 7
ShotSeg 3 'admin-order.png'     'cap_k3.txt' 8
ShotSeg 4 'admin-dashboard.png' 'cap_k4.txt' 8

# ---------- 分镜5：26-34s 大屏GIF回切 ----------
$f5 = "color=c=0x0e1a2b:s=1080x1920:r=30:d=8[b];[0:v]fps=30,scale=1080:-2:flags=lanczos[f];[b][f]overlay=(W-w)/2:(H-h)/2[bg];[bg]" +
  (CapText 'cap_k5.txt' 64 1430) + ',' + (Wm) + ',' + (Fades 8) + ',format=yuv420p[v]'
WriteFilter 'f5.txt' $f5
$a = @('-stream_loop','-1','-i',"$src/demo-bigscreen.gif",
      '-f','lavfi','-t','8','-i','anullsrc=r=44100:cl=stereo',
      '-filter_complex_script','f5.txt',
      '-map','[v]','-map','1:a','-t','8') + $enc + @("$seg/s5.mp4")
Run $a 'seg5'

# ---------- 分镜6：34-42s 价格对比大字（纯色底） ----------
$f6 = "color=c=0x101826:s=1080x1920:r=30:d=8[bg];[bg]" +
  "drawtext=fontfile='font_bold.ttc':text='商业 TMS：3~30 万/年':fontcolor=0xff9b4d:fontsize=78:x=(w-text_w)/2:y=740," +
  "drawtext=fontfile='font_bold.ttc':text='这套开源：0 元':fontcolor=0x3ddc84:fontsize=128:x=(w-text_w)/2:y=930," +
  (Wm) + ',' + (Fades 8) + ',format=yuv420p[v]'
WriteFilter 'f6.txt' $f6
$a = @('-f','lavfi','-t','8','-i','anullsrc=r=44100:cl=stereo',
      '-filter_complex_script','f6.txt',
      '-map','[v]','-map','0:a','-t','8') + $enc + @("$seg/s6.mp4")
Run $a 'seg6'

# ---------- 分镜7：42-45s Gitee 尾板 ----------
$f7 = "color=c=0x0e1a2b:s=1080x1920:r=30:d=3[bg];[bg]" +
  "drawtext=fontfile='font_bold.ttc':text='源码在 Gitee 主页':fontcolor=white:fontsize=70:x=(w-text_w)/2:y=660," +
  "drawtext=fontfile='font_reg.ttc':text='gitee.com/itxinfei/xinfei-tms':fontcolor=0xffd54d:fontsize=56:x=(w-text_w)/2:y=800," +
  "drawtext=fontfile='font_bold.ttc':text='学习免费 · 商用私信':fontcolor=white:fontsize=80:x=(w-text_w)/2:y=1000," +
  (Wm) + ',fade=t=in:st=0:d=0.25,format=yuv420p[v]'
WriteFilter 'f7.txt' $f7
$a = @('-f','lavfi','-t','3','-i','anullsrc=r=44100:cl=stereo',
      '-filter_complex_script','f7.txt',
      '-map','[v]','-map','0:a','-t','3') + $enc + @("$seg/s7.mp4")
Run $a 'seg7'

# ---------- 拼接成片 ----------
$list = 1..7 | ForEach-Object { "file '$seg/s$_.mp4'" }
[System.IO.File]::WriteAllLines("$work/list.txt", $list, (New-Object System.Text.UTF8Encoding($false)))
$out = "$doc/promo-video/topic1_final.mp4"
Run @('-f','concat','-safe','0','-i','list.txt','-c','copy',"$out") 'concat'
Write-Host "DONE -> $out"
