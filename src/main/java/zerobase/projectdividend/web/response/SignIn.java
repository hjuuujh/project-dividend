package zerobase.projectdividend.web.response;

import lombok.*;

public class SignIn {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String token;

        public static Response from(String token) {
            return Response.builder()
                    .token(token)
                    .build();
        }

    }
}
