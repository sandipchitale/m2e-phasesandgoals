# m2e-phasesandgoals
Fragment for M2E Core UI 
This extends M2E Core UI with commands related to Phases and Goals.

## Show Phases and Goals

Select a Maven project in the IDE and then invoke `Project > Phases and Goals` command. It shows all the phases ![](org.eclipse.m2e.core.ui.phasesandgoals/icons/phase.png) and goals ![](org.eclipse.m2e.core.ui.phasesandgoals/icons/goal.png) in a checkbox tree dialog.

![Screenshot](org.eclipse.m2e.core.ui.phasesandgoals/phasesandgoals.png)

- You can run the selected goals using the `Launch selected goals` ![](org.eclipse.m2e.core.ui.phasesandgoals/icons/launch.png) command.
- Expand All ![](org.eclipse.m2e.core.ui.phasesandgoals/icons/expand_all.png)/Collapse All ![](org.eclipse.m2e.core.ui.phasesandgoals/icons/collapse_all.png)
- Use the `Log All` ![](org.eclipse.m2e.core.ui.phasesandgoals/icons/log.png) command to print the tree into the Maven Console.
- Selecting Phase selects associated goals
- In Single Selection Mode, the goals are added to the set in the order in which they were selected. This allows you to run the goals in any order you like.
 
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
