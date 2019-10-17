# Mobile-Sleep-Detection

My project goal is to use a mobile phone or wearable device to detect
sleep and wirelessly turns off entertainment appliances when it detects they are being not used.
One online poll indicates that nearly 61% of people in the United States habitually leave their
television or appliances turned on when falling asleep. An average, modern television set alone
wastes up to 108 kWh every year, if left on for 8 hours each night. Projecting this energy waste
at a nation-wide scale, or further at world-wide scale, this reveals enormous amounts of energy
being wasted.

There are existing amenities available that can already allow these appliances to turn off
as a user falls asleep. Some of these appliances have built-in sleep timers which require menu-
driven setting changes on the television set. Also, there are devices that can disconnect power
at the user’s command such as the “The Clapper” (“clap-on-clap-off”). These amenities are
highly inconvenient and impractical to use, given that people tend to feel tired and their minds
are not in good working condition when falling asleep. People mostly forget to set the timer,
since many problems involve feeling too tired to go through the lengthy menu process of turning
on the sleep timer or setting overestimated times and leaving them on even after they fall
asleep. These existing amenities are too demanding and inconvenient to use on a regular daily
basis.

My project will present a proactive and intelligent approach to address this issue using a
mobile device together with a wireless controllable power switch apparatus. My project intends
to confront three major problems globally. It will contribute to conserving our environment by
reducing energy waste, while helping financially individual households to reduce their electric
bill. Consequently, people can achieve better sleep by timely turning off lighting and sounds in
the room. This project can contribute toward improving the general health of all individuals by
accommodating a better sleeping environment.

![alt text](https://github.com/matthewparkbusiness/Mobile-Sleep-Detection/blob/master/preview1.jpg)

My device design is composed of two sub-units: a mobile phone or tablet with a software that can detect sleep
status of individuals and a wirelessly controllable power-switch-device that can connect or
disconnect power to the appliances.

For the mobile device software portion of my project, the device is required to have a
speaker, microphone, camera, and blue-tooth hardware. Optionally, this unit may pair with a
wearable device, such as an android smart watch. The software will detect the sleep status by
one of the following methods or by a combination of three methods: volume-controlled audio
interaction with the individuals via speaker/microphone, face-detection via camera, or heart rate
monitoring via the wearable. The software communicates with the other sub-unit (below) via
blue-tooth technology. This software component will be programmed in Java on Android Studio
and will not require any exceptionally high performing hardware components. Almost all Android
devices in the market should be able to support the software described above.

This power-switch-device consists of a solid-state relay, an IO-board (Arduino board), a
Bluetooth transceiver module, and a power supply. These components do not have to be any
high-performing ones since speed is not an important factor for the functionality of this device.
This device will be roughly 4&quot;x4&quot;x1&quot; and may vary in the course of the progression of this
project.  Although depending on the frame, the shape of my device will most likely be a
rectangle and should weigh 1-3 lbs., given that my design should be merely attached to power
outlets. I estimate that this project will take 150-200 hours, along with at maximum $150-$200
purchases for the Android device and the power-switch-device components.
