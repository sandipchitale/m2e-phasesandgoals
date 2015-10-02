# Maven build spy

A simple Maven Build Spy. It shows the success or failure of the goals, the timing and the exception message as a tooltip for failed goals.

![Screenshot](../org.eclipse.m2e.core.ui.phasesandgoals/mavenbuildspy.png)

You can use the spy in your Maven builds by:

- Downloading the [Maven Build Spy jar](https://github.com/sandipchitale/m2e-phasesandgoals/blob/master/org.eclipse.m2e.core.ui.phasesandgoals/mavenbuildspy/mavenbuildspy.jar).
- And then passing the following parameter to your mvn build like so:

`> mvn -Dmaven.ext.class.path=path-to/mavenbuildspy.jar ....`

- Alternatively you can simply copy the `mavenbuildspy.jar` to `lib/ext` folder of your maven installation.
