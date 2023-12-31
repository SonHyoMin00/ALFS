"use client";

import React, { useEffect, useState } from "react";
import { AiOutlineShoppingCart } from "react-icons/ai";
import { AiOutlineEnvironment } from "react-icons/ai";
import { MdOutlineFoodBank } from "react-icons/md";
import { Button, Tooltip } from "@chakra-ui/react";
import Link from "next/link";

import {
  Popover,
  PopoverTrigger,
  PopoverContent,
  PopoverHeader,
  PopoverBody,
  PopoverFooter,
  PopoverArrow,
  PopoverCloseButton,
} from "@chakra-ui/react";
import { AddressAll } from "@/app/api/user/user";

export default function MiddleIcon() {
  const [address, setAddress] = useState<any>({});
  const [memberId, setMemberId] = useState<any>("");
  const myAddress = async (id: any) => {
    const res = await AddressAll(id);
    const original = res?.data.data.filter((item: any) => item.status === true);
    setAddress(original);
  };

  useEffect(() => {
    const id = localStorage.getItem("id");
    setMemberId(id);
    if (id) {
      myAddress(id);
    }
  }, []);
  return (
    <div className="flex gap-[20px]">
      <Tooltip label="배송지" placement="top">
        <span>
          <Popover>
            <PopoverTrigger>
              <Button width="40px" height="40px" variant="unstyled">
                <AiOutlineEnvironment className="min-w-[40px] min-h-[40px]" />
              </Button>
            </PopoverTrigger>
            <PopoverContent>
              <PopoverArrow />
              <PopoverCloseButton />
              {memberId ? (
                <>
                  <PopoverHeader>
                    <b className="text-[20px]">현재배송지</b>
                  </PopoverHeader>
                  <PopoverBody>
                    {address[0]?.address_1}
                    <br />
                    {address[0]?.address_2}
                  </PopoverBody>
                  <PopoverFooter justifyContent="end" display="flex">
                    <Link href="/mypage/home">
                      <Button variant="outline" colorScheme="whatsapp">
                        배송지 추가하러 가기
                      </Button>
                    </Link>
                  </PopoverFooter>
                </>
              ) : (
                <>
                  <PopoverHeader>
                    <b className="text-[20px]">현재배송지</b>
                  </PopoverHeader>
                  <PopoverBody>
                    배송지 정보가 없습니다.
                    <br />
                  </PopoverBody>
                  <PopoverFooter justifyContent="end" display="flex">
                    <Link href="/login">
                      <Button variant="outline" colorScheme="whatsapp">
                        로그인
                      </Button>
                    </Link>
                  </PopoverFooter>
                </>
              )}
            </PopoverContent>
          </Popover>
        </span>
      </Tooltip>
      <Link href={{ pathname: `/mypage/allergy` }}>
        <Tooltip label="알러지" placement="top">
          <span>
            <MdOutlineFoodBank className="min-w-[40px] h-[40px]" />
          </span>
        </Tooltip>
      </Link>
      <Link href={{ pathname: `/cart` }} id="cart">
        <Tooltip label="장바구니" placement="top">
          <span>
            <AiOutlineShoppingCart className="min-w-[40px] h-[40px]" />
          </span>
        </Tooltip>
      </Link>
    </div>
  );
}
