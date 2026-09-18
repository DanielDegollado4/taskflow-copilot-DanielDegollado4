Copilot review overview
🟡 Changes recommended
The controller test has a moderate coverage gap and uses the outdated bean override annotation.

Get a fresh assessment by requesting another Copilot review.

Review effort: Lite
Findings: 1 Low severity

Open (1)
Low severity Sustituir @​MockBean por @​MockitoBean en la prueba slice · New

Archivo: src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
Comment on lines +31 to +35
    @MockBean
    ProjectService projectService;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;