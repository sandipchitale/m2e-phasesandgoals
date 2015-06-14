# m2e-phasesandgoals
Fragment for M2E Core UI 
This extends M2E Core UI with commands related to Phases and Goals.

## Show Phases and Goals

Select a Maven project in the IDE and then invoke `Project > Phases and Goals` command. It shows all the phases and goals in a checkbox tree dialog.

![Screenshot](org.eclipse.m2e.core.ui.phasesandgoals/phasesandgoals.png)

- You can run the selected goals using the `Launch selected goals` command. 
- You can copy selected goals to clipboard using the `Copy selected goals to clipboard` command. 
- Use the `Log All` command to print the tree into the Maven Console.

### Maven enhancement

Ideally this should be available via standard Maven command-line. For example the

`mvn help:describe -Dcmd=package`

command should be enhanced in terms from listing the actual goals bound to the phases. That way you can list the goals and then invoke them (by including the execution id). And the same mechanism behind the enhancement should/could be used to implement something like:

`mvn test-compile pre-integration-test...post-integration-test`

That way you can only run all the goals associated with

- test-compile
- pre-integration-test
- integration-test (due to ... )
- post-integration-test
