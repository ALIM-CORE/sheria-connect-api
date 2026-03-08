package co.tz.sheriaconnectapi.utils;

public class EmailTemplateBuilder {

    private EmailTemplateBuilder() {}

    public static String buildActionEmail(
            String name,
            String title,
            String message,
            String buttonText,
            String buttonLink,
            String expiryText
    ) {
        return """
                <div style="font-family: Arial, sans-serif; line-height: 1.6; padding: 20px;">
                    <h2>Hello %s 👋</h2>
                    <h3>%s</h3>
                    <p>%s</p>
                    <p>
                        <a href="%s"
                           style="background-color:#1e88e5;
                                  color:white;
                                  padding:12px 18px;
                                  text-decoration:none;
                                  border-radius:6px;
                                  display:inline-block;">
                            %s
                        </a>
                    </p>
                    <p style="font-size: 14px; color: gray;">%s</p>
                    <br/>
                    <p>— Sheria Connect Team</p>
                </div>
                """.formatted(name, title, message, buttonLink, buttonText, expiryText);
    }
}
