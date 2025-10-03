package dk.northtech.dassco_specify_adapter.webapi;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@OpenAPIDefinition(
        info = @Info(
                title = "DaSSCo Sepcify Adapter",
                version = "1.0",
                description = """
      DaSSCO Sepcify Adapter API Documentation
      """
        ),
        servers = {
                @Server(url = "${apiServerUrl}"),
        }
)
@Path("/")
public class OpenAPI {

        @GET
        @Hidden
        @Path("openapi.json")
        @Produces(APPLICATION_JSON)
        public String json(@Context HttpServletRequest request) {
                return replaceServers(request, readFromClasspath("/openapi.json"));
        }

        @GET
        @Hidden
        @Path("openapi.yaml")
        @Produces("text/plain; charset=utf-8")
        public String yaml(@Context HttpServletRequest request) {
                return replaceServers(request, readFromClasspath("/openapi.yaml"));
        }


        static String readFromClasspath(String resourceName) {
                try (var in = OpenAPI.class.getResourceAsStream(resourceName)) {
                        if (in == null) {
                                throw new UncheckedIOException(new IOException("No " + resourceName + " in classpath"));
                        }
                        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException iox) {
                        throw new UncheckedIOException(iox);
                }
        }

        private String replaceServers(HttpServletRequest request, String s) {
                if (request != null) {
                        var server = request.getScheme() + "://" + request.getServerName();
                        if (request.getServerPort() != 80) {
                                server += ":" + request.getServerPort();
                        }

                        var authServerUrlPattern = Pattern.compile("\\$\\{authServerUrl}");
                        var clientIdPattern = Pattern.compile("\\$\\{authClientId}");
                        var apiServerUrlPattern = Pattern.compile("\\$\\{apiServerUrl}");

                }
                return s;
        }
}
