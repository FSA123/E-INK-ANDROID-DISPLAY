Dependency Notes:
xt-deviceconnector: Peripheral communication component supporting USB and serial port communication. Serial port communication requires importing libserial_port.so.
xt-common: TCon common foundational library.
xt-t1000Core: Image processing core library for T1000 series devices, requiring import of libT1000Core.so.
xt-t1000: T1000 image displaying library, depends on xt-common and xt-t1000Core
xt-t2000Core: T2000 core library for T2000 series devices, requiring import of libT2000Core.
xt-t2000: T2000 image displaying library, depends on xt-common ,xt-t1000Core and xt-t2000Core
xt-imgrender (optional): Eink E6 image rendering component, requires importing libEink_Spectra6_render_JNI.so


Dependency update notes:
xt-deviceconnector-1.1.0.jar: Supports 16 KB page sizes
xt-common-1.0.2.jar: Resolved the issue where the E6 algorithm failed to switch
xt-t1000Core-1.0.3.jar: Supports 16 KB page sizes; Optimises E6 display
xt-imgrender-1.0.3.jar: Optimises E6 display