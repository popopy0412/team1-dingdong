import styled from "styled-components";
import {colors} from "@/styles/colors.ts";
import {Body1SemiBold} from "@/styles/typography.ts";


export const Wrapper = styled.div`
    position: sticky;
    top: 0;
    
    display: flex;
    height: 60px;
    padding: 0 20px;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
    border-bottom: 1px solid ${colors.gray20};
    background: ${colors.gray0};
`


export const Title = styled(Body1SemiBold)`
    color: ${colors.gray100};

    /* Body 1/Semibold */
    font-family: Pretendard,serif;
    font-style: normal;
`

export const ActionButton = styled.button`
    width: 24px;
    height: 24px;
    flex-shrink: 0;
`